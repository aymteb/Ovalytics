package com.ovalytics.backend.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.import.prod2")
public class Prod2MatchImportProperties {

	private String file = "";

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
