package com.ovalytics.backend.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.import.news")
public class NewsImportProperties {

	private String file = "";
	private int retainDays = 7;

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public int getRetainDays() {
		return retainDays;
	}

	public void setRetainDays(int retainDays) {
		this.retainDays = retainDays;
	}
}
