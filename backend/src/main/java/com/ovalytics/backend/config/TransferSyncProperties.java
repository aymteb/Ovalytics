package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.transfer-sync")
public class TransferSyncProperties {

	private boolean enabled = false;
	private String cron = "0 0 12,20 * * *";
	private boolean scrapeEnabled = true;
	private String repoRoot = "..";
	private String pythonCommand = "python3";
	private String output = "data/import/transfers.csv";

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

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}
}
