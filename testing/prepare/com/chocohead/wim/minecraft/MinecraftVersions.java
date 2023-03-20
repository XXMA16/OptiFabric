package com.chocohead.wim.minecraft;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.system.JkLocator;

import com.chocohead.wim.Main;

public class MinecraftVersions {
	public static class Version {
		String id;
		private String url;

		public MinecraftVersion load() throws IOException {
			Path json = CACHE.resolve(id + ".json");
			JkUrlFileProxy.of(url, json).get();

			try (Reader reader = Files.newBufferedReader(json)) {
				return Main.GSON.fromJson(reader, MinecraftVersion.class);
			}
		}
	}
	static final transient Path CACHE = JkLocator.getCacheDir().resolve("minecraft");
	private List<Version> versions = new ArrayList<>();

	public static MinecraftVersions load() throws IOException {
		Path json = CACHE.resolve("version-manifest.json");
		JkUrlFileProxy.of("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", json).get();

		try (Reader reader = Files.newBufferedReader(json)) {
			return Main.GSON.fromJson(reader, MinecraftVersions.class);
		}
	}

	public List<Version> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public Version getVersion(String id) {
		for (Version version : versions) {
			if (version.id.equals(id)) {
				return version;
			}
		}

		return null;
	}
}