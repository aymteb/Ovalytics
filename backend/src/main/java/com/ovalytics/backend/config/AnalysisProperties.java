package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.analysis")
public class AnalysisProperties {

	private boolean enabled = true;
	private String cron = "0 0 9 * * TUE";
	private int windowDays = 7;
	private boolean catchUpOnStartup = false;
	private String apiKey = "";
	private String baseUrl = "https://api.openai.com/v1";
	private String model = "gpt-4o-mini";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public int getWindowDays() {
		return windowDays;
	}

	public void setWindowDays(int windowDays) {
		this.windowDays = windowDays;
	}

	public boolean isCatchUpOnStartup() {
		return catchUpOnStartup;
	}

	public void setCatchUpOnStartup(boolean catchUpOnStartup) {
		this.catchUpOnStartup = catchUpOnStartup;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
}
