package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.h2h-sync")
public class H2hSyncProperties {

	private boolean enabled = false;
	private String cron = "0 0 7 * * *";
	private boolean scrapeEnabled = true;
	private String repoRoot = "..";
	private String pythonCommand = "python3";
	private String top14Output = "data/import/top14-h2h.csv";
	private String prod2Output = "data/import/prod2-h2h.csv";
	private String nationaleOutput = "data/import/nationale-h2h.csv";
	private String erccOutput = "data/import/champions-cup-h2h.csv";
	private String erchOutput = "data/import/challenge-cup-h2h.csv";
	private String urcOutput = "data/import/urc-h2h.csv";
	private String premOutput = "data/import/premiership-h2h.csv";
	private String eventIdsFile = "data/import/h2h-event-ids.txt";
	private int daysAhead = 45;

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

	public String getNationaleOutput() {
		return nationaleOutput;
	}

	public void setNationaleOutput(String nationaleOutput) {
		this.nationaleOutput = nationaleOutput;
	}

	public String getErccOutput() {
		return erccOutput;
	}

	public void setErccOutput(String erccOutput) {
		this.erccOutput = erccOutput;
	}

	public String getErchOutput() {
		return erchOutput;
	}

	public void setErchOutput(String erchOutput) {
		this.erchOutput = erchOutput;
	}

	public String getUrcOutput() {
		return urcOutput;
	}

	public void setUrcOutput(String urcOutput) {
		this.urcOutput = urcOutput;
	}

	public String getPremOutput() {
		return premOutput;
	}

	public void setPremOutput(String premOutput) {
		this.premOutput = premOutput;
	}

	public String getEventIdsFile() {
		return eventIdsFile;
	}

	public void setEventIdsFile(String eventIdsFile) {
		this.eventIdsFile = eventIdsFile;
	}

	public int getDaysAhead() {
		return daysAhead;
	}

	public void setDaysAhead(int daysAhead) {
		this.daysAhead = daysAhead;
	}
}
