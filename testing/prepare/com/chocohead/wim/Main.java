package com.chocohead.wim;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.tinylog.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jsoup.helper.HttpConnection;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathTree;

import com.chocohead.wim.minecraft.Minecraft;

public class Main {
	public static class Settings {
		String version;
		String optifine;
		List<String> jars;
		Map<String, String> config;
		Map<String, String> overrides;
	}
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	public static void main(String[] args) {
		if (args != null && args.length >= 1 && args[0] != null) {
			String command = args[0];
			args = Arrays.copyOfRange(args, 1, args.length);

			switch (command) {
			case "prepare":
				prepare(args);
				return;
			case "setup":
				setup(args);
				return;
			}	
		}

		Logger.error("Expected one of");
		Logger.error("\tprepare <version> <run dir> <provider path>");
		Logger.error("\tsetup <settings> <run dir>");
		System.exit(-1);
	}

	private static void prepare(String... args) {
		if (args.length != 3) {
			Logger.error("Expected prepare <version> <run dir> <provider path> but received {} arguments", args.length);
			System.exit(-1);
			return;
		}
		String version = args[0];
		JkPathTree runDir = JkPathTree.of(args[1]);
		Path provider = Paths.get(args[2]);

		List<Path> minecraft = Minecraft.fetch(version);
		List<Path> fabric = Fabric.fetch(version, "0.14.17");
		Path launcherJar = JkRepoSet.of("https://maven.fabricmc.net/").get("net.fabricmc:dev-launch-injector:0.2.1+build.8");

		runDir.importFile(launcherJar, "launcher.jar", StandardCopyOption.REPLACE_EXISTING);
		copyIfMissing(Stream.concat(minecraft.stream(), Stream.concat(fabric.stream(), Stream.of(provider))), runDir.goTo("launch-libs"));
		assert runDir.andMatching("launch-libs/*").getRelativeFiles().stream().noneMatch(path -> path.toString().indexOf(' ') >= 0);
		try (JkPathTree launcher = JkPathTree.ofZip(runDir.get("launcher.jar"));
				OutputStream out = Files.newOutputStream(launcher.get("META-INF/MANIFEST.MF"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().put(Name.MAIN_CLASS, "net.fabricmc.devlaunchinjector.Main");
			manifest.getMainAttributes().put(Name.CLASS_PATH, runDir.andMatching("launch-libs/*").getRelativeFiles().stream().map(Path::toString).collect(Collectors.joining(" ")).replace('\\', '/'));
			manifest.write(out);
		} catch (IOException e) {
			throw new UncheckedIOException("Error writing launcher jar", e);
		}
	}

	private static void setup(String... args) {
		if (args.length != 2) {
			Logger.error("Expected setup <settings> <run dir> but received {} arguments", args.length);
			System.exit(-1);
			return;
		}
		Settings settings = GSON.fromJson(args[0], Settings.class);
		JkPathTree runDir = JkPathTree.of(args[1]);

		System.setProperty("http.agent", HttpConnection.DEFAULT_UA);
		Path optifine = OptiFine.fetch(settings.version, settings.optifine);
		List<Path> jars = resolveJars(settings);

		runDir.andMatching("mods/*", "config/*").deleteContent();
		runDir.goTo("mods").importFile(optifine, "OptiFine.jar").importFiles(jars);
		if (settings.config != null || settings.overrides != null) {
			JkPathTree config = runDir.goTo("config").createIfNotExist();

			if (settings.config != null) {
				//TODO
			}
			if (settings.overrides != null) {
				try (Writer out = Files.newBufferedWriter(config.get("fabric_loader_dependencies.json"))) {
					Map<String, Object> file = new HashMap<>();
					file.put("version", 1);
					file.put("overrides", settings.overrides);
					GSON.toJson(file, out);
				} catch (IOException e) {
					throw new UncheckedIOException("Error writing dependency overrides", e);
				}
			}
		}
	}

	private static List<Path> resolveJars(Settings settings) {
		JkDependencySet dependencies = JkDependencySet.of(settings.jars.stream().map(JkModuleDependency::of).collect(Collectors.toList()));

		JkResolveResult result = JkDependencyResolver.of().setRepos(JkRepoSet.of("https://maven.fabricmc.net/", "https://cursemaven.com/")).resolve(dependencies);
		if (result.getErrorReport().hasErrors()) {
			Logger.error(result.getErrorReport().toString());
			throw new RuntimeException("Unable to download all jars");
		}

		return result.getFiles().getEntries();
	}

	private static void copyIfMissing(Stream<Path> from, JkPathTree to) {
		to.importFiles(from.filter(path -> Files.notExists(to.getRoot().resolve(path.getFileName()))).collect(Collectors.toList()));
	}
}