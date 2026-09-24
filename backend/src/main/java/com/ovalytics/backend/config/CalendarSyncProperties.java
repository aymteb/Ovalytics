package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.calendar-sync")
public class CalendarSyncProperties {

	private boolean enabled = false;
	private String cron = "0 30 6 * * *";
	private boolean scrapeEnabled = true;
	private String repoRoot = "..";
	private String pythonCommand = "python3";
	private String top14Output = "data/import/top14-matches.csv";
	private String prod2Output = "data/import/prod2-matches.csv";

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

	public boolean isScrapeEnabled() {
		return scrapeEnabled;
	}

	public void setScrapeEnabled(boolean scrapeEnabled) {
		this.scrapeEnabled = scrapeEnabled;
	}

	public String getRepoRoot() {
		return repoRoot;
	}

	public void setRepoRoot(String repoRoot) {
		this.repoRoot = repoRoot;
	}

	public String getPythonCommand() {
		return pythonCommand;
	}

	public void setPythonCommand(String pythonCommand) {
		this.pythonCommand = pythonCommand;
	}

	public String getTop14Output() {
		return top14Output;
	}

	public void setTop14Output(String top14Output) {
		this.top14Output = top14Output;
	}

	public String getProd2Output() {
		return prod2Output;
	}

	public void setProd2Output(String prod2Output) {
		this.prod2Output = prod2Output;
	}
}
