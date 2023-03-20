package com.chocohead.wim.minecraft;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.tinylog.Logger;

import com.google.gson.JsonSyntaxException;

import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.utils.JkUtilsString;

import com.chocohead.wim.minecraft.MinecraftVersion.Library;

public class Minecraft {
	public static List<Path> fetch(String version) {
		Logger.debug("Fetching Minecraft {}", version);
		MinecraftVersion json;
		try {
			json = Objects.requireNonNull(MinecraftVersions.load().getVersion(version), "Didn't find " + version + " in manifest").load();
		} catch (JsonSyntaxException e) {
			throw new IllegalStateException("Failed to parse Minecraft manifest?", e); //We shouldn't be getting invalid JSON
		} catch (IOException e) {
			throw new UncheckedIOException("Error downloading Minecraft manifest", e);
		}
		Logger.debug("Resolving client jar");
		Path game = JkUrlFileProxy.of(Objects.requireNonNull(json.downloads.get("client"), "No client download?").url, MinecraftVersions.CACHE.resolve(version + ".jar")).get();

		Logger.debug("Resolving libraries");
		List<Path> libaries = resolveLibaries(json);
		libaries.add(0, game);
		return libaries;
	}

	private static List<Path> resolveLibaries(MinecraftVersion json) {
		List<Path> libraries = new ArrayList<>();
		JkRepoSet repo = JkRepoSet.of("https://libraries.minecraft.net", JkRepo.MAVEN_CENTRAL_URL);

		for (Library library : json.libraries) {
			if (!library.shouldUse()) continue;

			JkModuleDependency dependency;
			switch (JkUtilsString.countOccurrence(library.name, ':')) {
				case 2:
					dependency = JkModuleDependency.of(library.name);
					break;

				case 3: {
					String[] parts = library.name.split(":", 4); //Jeka reads classifiers and type before version
					dependency = JkModuleDependency.of(parts[0], parts[1], parts[2]).withClassifiers(parts[3]);
					break;
				}

				default:
					throw new IllegalStateException("Expected 2 or 3 colons in library name, but found " + library.name);
			}

			if (library.downloads.artifact != null) {//Some libraries (like LWJGL-Platform) only have native artifacts
				libraries.add(repo.get(dependency));
			}

			if (library.hasNative()) {//Shouldn't need these unless the game is actually started 
				//dependencies.add(dependency.withClassifiers(Objects.requireNonNull(library.getNative(), library.name + " has null native")));
			}
		}

		return libraries;
	}
}