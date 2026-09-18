package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.cors")
public class CorsProperties {

	private String allowedOrigins = "http://localhost:4200";

	public String getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public String[] origins() {
		if (allowedOrigins == null || allowedOrigins.isBlank()) {
			return new String[] { "http://localhost:4200" };
		}
		String[] parts = allowedOrigins.split(",");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		return parts;
	}
}
