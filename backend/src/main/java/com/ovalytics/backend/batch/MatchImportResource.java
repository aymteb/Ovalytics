package com.ovalytics.backend.batch;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

final class MatchImportResource {

	private static final String DEFAULT_MATCH_CLASSPATH = "data/top14-import.csv";

	private MatchImportResource() {
	}

	static Resource resolve(String file, ResourceLoader resourceLoader) {
		return resolve(file, resourceLoader, DEFAULT_MATCH_CLASSPATH);
	}

	static Resource resolve(String file, ResourceLoader resourceLoader, String defaultClasspath) {
		if (file == null || file.isBlank()) {
			return new ClassPathResource(defaultClasspath);
		}
		String location = file.trim();
		if (location.startsWith("classpath:") || location.startsWith("file:")) {
			return resourceLoader.getResource(location);
		}
		return new FileSystemResource(location);
	}
}
