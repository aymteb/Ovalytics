package com.ovalytics.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotEnvLoader {

	private DotEnvLoader() {
	}

	public static void loadQuietly() {
		for (Path path : List.of(Path.of(".env"), Path.of("../.env"))) {
			if (Files.isRegularFile(path)) {
				loadFile(path);
				break;
			}
		}
		if (!hasGeminiApiKey()) {
			System.setProperty("spring.ai.model.chat", "none");
		}
	}

	public static boolean hasGeminiApiKey() {
		return hasValue("OVALYTICS_ANALYSIS_API_KEY") || hasValue("GEMINI_API_KEY");
	}

	private static boolean hasValue(String key) {
		String env = System.getenv(key);
		if (env != null && !env.isBlank()) {
			return true;
		}
		String prop = System.getProperty(key);
		return prop != null && !prop.isBlank();
	}

	private static void loadFile(Path path) {
		try {
			for (String raw : Files.readAllLines(path)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int sep = line.indexOf('=');
				String key = line.substring(0, sep).trim();
				String value = stripQuotes(line.substring(sep + 1).trim());
				if (key.isEmpty()) {
					continue;
				}
				if (System.getenv(key) != null) {
					continue;
				}
				if (System.getProperty(key) != null) {
					continue;
				}
				System.setProperty(key, value);
			}
		} catch (IOException ignored) {
		}
	}

	private static String stripQuotes(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}
}
