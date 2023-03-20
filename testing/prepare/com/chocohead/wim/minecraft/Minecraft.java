package com.chocohead.wim.minecraft;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.tinylog.Logger;

import com.google.gson.JsonSyntaxException;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
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
		JkResolveResult libaries = resolveLibaries(json);
		if (libaries.getErrorReport().hasErrors()) {
			Logger.error(libaries.getErrorReport().toString());
			throw new RuntimeException("Unable to download all " + version + " libraries");
		}

		List<Path> out = new ArrayList<>(libaries.getFiles().getEntries());
		out.add(0, game);
		return out;
	}

	private static JkResolveResult resolveLibaries(MinecraftVersion json) {
		List<JkDependency> dependencies = new ArrayList<>();

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
				dependencies.add(dependency);
			}

			if (library.hasNative()) {//Shouldn't need these unless the game is actually started 
				//dependencies.add(dependency.withClassifiers(Objects.requireNonNull(library.getNative(), library.name + " has null native")));
			}
		}

		return JkDependencyResolver.of().setRepos(JkRepoSet.of(JkRepo.ofMavenCentral(), JkRepo.of("https://libraries.minecraft.net"))).resolve(JkDependencySet.of(dependencies));
	}
}