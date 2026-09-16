package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.team-refresh")
public class TeamRefreshProperties {

	private boolean enabled = true;
	private String cron = "0 0 23 * * *";
	private boolean scrapeEnabled = true;
	private String repoRoot = "..";
	private String profilesOutput = "data/import/player-profiles.csv";
	private String pythonCommand = "python3";

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

	public String getProfilesOutput() {
		return profilesOutput;
	}

	public void setProfilesOutput(String profilesOutput) {
		this.profilesOutput = profilesOutput;
	}

	public String getPythonCommand() {
		return pythonCommand;
	}

	public void setPythonCommand(String pythonCommand) {
		this.pythonCommand = pythonCommand;
	}
}
