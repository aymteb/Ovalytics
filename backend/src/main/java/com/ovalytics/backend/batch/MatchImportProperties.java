package com.ovalytics.backend.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.import")
public class MatchImportProperties {

	private String file = "";

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
