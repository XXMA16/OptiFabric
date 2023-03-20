package com.chocohead.wim;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class Provider extends MinecraftGameProvider {
	private static final String ENTRYPOINT = "com.chocohead.wim.Start";

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		URL url = ClassLoader.getSystemResource("META-INF/MANIFEST.MF");
		if (url == null) return false;

		String classPath;
		try (InputStream in = url.openStream()) {
			classPath = new Manifest(in).getMainAttributes().getValue(Name.CLASS_PATH);
		} catch (IOException e) {
			Log.error(LogCategory.GAME_PROVIDER, "Error reading manifest from %s", url, e);
			return false;
		}

		//Strictly speaking the class path should be made up of space separated URLs
		//Having already directly converted the paths to strings, there won't be any URL escaping
		//So we'll pragmatically decide that there are no spaces within the paths themselves
		System.setProperty("java.class.path", classPath.replace(' ', File.pathSeparatorChar));
		for (String entry : classPath.split(" ")) {
			Path path = Paths.get(entry);

			if (Files.exists(path)) {
				launcher.getClassPath().add(LoaderUtil.normalizeExistingPath(path));
			} else {
				Log.warn(LogCategory.GAME_PROVIDER, "Missing file from class path: %s", entry);
			}
		}

		return super.locateGame(launcher, args);
	}

	@Override
	public void launch(ClassLoader loader) {
		try {
			Class<?> c = loader.loadClass(ENTRYPOINT);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) getArguments().toArray());
		} catch (InvocationTargetException e) {
			throw new FormattedException("Launcher has crashed", e.getCause()); //Pass it on
		} catch (ReflectiveOperationException e) {
			throw new FormattedException("Failed to start Launcher", e);
		}
	}

	@Override
	public boolean canOpenErrorGui() {
		return false; //We're working headlessly
	}
}