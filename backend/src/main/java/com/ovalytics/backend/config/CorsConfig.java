package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

	private final CorsProperties corsProperties;

	public CorsConfig(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String[] origins = corsProperties.origins();
		var registration = registry.addMapping("/api/**")
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
		if (origins.length == 1 && "*".equals(origins[0])) {
			registration.allowedOriginPatterns("*");
		} else {
			registration.allowedOrigins(origins);
		}
	}
}