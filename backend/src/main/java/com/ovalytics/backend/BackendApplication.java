package com.ovalytics.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ovalytics.backend.config.DotEnvLoader;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		DotEnvLoader.loadQuietly();
		DatasourceBootstrap.apply();
		SpringApplication.run(BackendApplication.class, args);
	}

}
