package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.live-scores")
public class LiveScoreProperties {

	private boolean enabled = true;
	private long pollIntervalMs = 60_000L;
	private String userAgent = "Ovalytics/1.0";
	private String incrowdApiKey = "zDiLQ9o18oVrwn30etwT";
	private String incrowdRealm = "prl";
	private String incrowdAppId = "web";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getIncrowdApiKey() {
		return incrowdApiKey;
	}

	public void setIncrowdApiKey(String incrowdApiKey) {
		this.incrowdApiKey = incrowdApiKey;
	}

	public String getIncrowdRealm() {
		return incrowdRealm;
	}

	public void setIncrowdRealm(String incrowdRealm) {
		this.incrowdRealm = incrowdRealm;
	}

	public String getIncrowdAppId() {
		return incrowdAppId;
	}

	public void setIncrowdAppId(String incrowdAppId) {
		this.incrowdAppId = incrowdAppId;
	}
}
