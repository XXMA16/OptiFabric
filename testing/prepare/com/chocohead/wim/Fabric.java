package com.chocohead.wim;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.google.gson.JsonSyntaxException;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.system.JkLocator;

public class Fabric {
	public static class Profile {
		public static class Library {
			public String name;
			public String url;
		}

		public List<Library> libraries;
	}

	public static List<Path> fetch(String minecraft, String version) {
		Logger.debug("Fetching Fabric Loader {} for {}", version, minecraft);
		Path cache = JkLocator.getCacheDir().resolve("fabric");

		Path json = cache.resolve("loader-" + version + '-' + minecraft + ".json");
		JkUrlFileProxy.of("https://meta.fabricmc.net/v2/versions/loader/" + minecraft + '/' + version + "/profile/json", json).get();
		Profile profile;
		try (Reader reader = Files.newBufferedReader(json)) {
			profile = Main.GSON.fromJson(reader, Profile.class);
		} catch (JsonSyntaxException e) {
			throw new IllegalStateException("Failed to parse Fabric Loader profile?", e); //We shouldn't be getting invalid JSON
		} catch (IOException e) {
			throw new UncheckedIOException("Error downloading Fabric Loader profile", e);
		}

		Logger.debug("Resolving Fabric Loader libraries");
		List<JkModuleDependency> libraries = profile.libraries.stream().map(library -> JkModuleDependency.of(library.name)).collect(Collectors.toList());
		JkResolveResult result = JkDependencyResolver.of().setRepos(JkRepoSet.of("https://maven.fabricmc.net/")).resolve(JkDependencySet.of(libraries));
		if (result.getErrorReport().hasErrors()) {
			Logger.error(result.getErrorReport().toString());
			throw new RuntimeException("Unable to download all Fabric Loader " + version + " libraries");
		}

		return result.getFiles().getEntries();
	}
}