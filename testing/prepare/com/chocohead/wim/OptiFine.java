package com.chocohead.wim;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.tinylog.Logger;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsPath;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class OptiFine {
	public static Path fetch(String minecraft, String version) {
		Path to = optifineCache(minecraft).resolve(version.concat(".jar"));

		if (!Files.isRegularFile(to)) {
			Logger.debug("OptiFine not present at {}, downloading...", to);
			try {
				String url = fishDownload(minecraft, version);
				Logger.info("Downloading {}", url);
				JkPathFile.of(to).fetchContentFrom(url).get();
			} catch (IOException e) {
				throw new UncheckedIOException("Error downloading OptiFine", e);
			}
		} else Logger.debug("OptiFine found at {}", to);

		return to;
	}

	private static String fishDownload(String minecraft, String version) throws IOException {
		String url = String.format("https://optifine.net/adloadx?f=%sOptiFine_%s_%s.jar", version.contains("_pre") ? "preview_" : "", minecraft, version);
		Logger.info("Downloading {}", url);
		Document doc = Jsoup.connect(url).timeout(5000).get();

		Element downloadRegion = Objects.requireNonNull(doc.getElementById("Download"), "Couldn't find download region on page");
		for (Element a : downloadRegion.getElementsByTag("a")) {
			String link = a.attr("abs:href");
			if (link != null) return link;
		}
		throw new IllegalStateException("Unable to find download link on page");
	}

	public static Path optifineCache(String minecraft) {
		Path out = JkLocator.getCacheDir().resolve("optifine").resolve(minecraft);
		JkUtilsPath.createDirectories(out);
		return out;
	}
}