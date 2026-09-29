package com.ovalytics.backend.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.import")
public class MatchImportProperties {

	private String file = "";
	private String h2hFile = "";

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getH2hFile() {
		return h2hFile;
	}

	public void setH2hFile(String h2hFile) {
		this.h2hFile = h2hFile;
	}
}
