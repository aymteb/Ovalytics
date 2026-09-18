package com.ovalytics.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		normalizeDatasourceUrl();
		SpringApplication.run(BackendApplication.class, args);
	}

	static void normalizeDatasourceUrl() {
		String url = firstNonBlank(
				System.getenv("SPRING_DATASOURCE_URL"),
				System.getenv("DATABASE_URL"),
				System.getProperty("spring.datasource.url"));
		if (url == null) {
			return;
		}
		String normalized = url.trim();
		if (normalized.startsWith("postgres://")) {
			normalized = "jdbc:postgresql://" + normalized.substring("postgres://".length());
		} else if (normalized.startsWith("postgresql://")) {
			normalized = "jdbc:" + normalized;
		}
		if (!normalized.equals(url)) {
			System.setProperty("spring.datasource.url", normalized);
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
