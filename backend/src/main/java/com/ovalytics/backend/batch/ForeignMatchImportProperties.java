package com.ovalytics.backend.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ovalytics.import.foreign")
public class ForeignMatchImportProperties {

	private String erccFile = "";
	private String erchFile = "";
	private String urcFile = "";
	private String premFile = "";
	private String intFile = "";
	private String erccH2hFile = "";
	private String erchH2hFile = "";
	private String urcH2hFile = "";
	private String premH2hFile = "";

	public String getErccFile() {
		return erccFile;
	}

	public void setErccFile(String erccFile) {
		this.erccFile = erccFile;
	}

	public String getErchFile() {
		return erchFile;
	}

	public void setErchFile(String erchFile) {
		this.erchFile = erchFile;
	}

	public String getUrcFile() {
		return urcFile;
	}

	public void setUrcFile(String urcFile) {
		this.urcFile = urcFile;
	}

	public String getPremFile() {
		return premFile;
	}

	public void setPremFile(String premFile) {
		this.premFile = premFile;
	}

	public String getIntFile() {
		return intFile;
	}

	public void setIntFile(String intFile) {
		this.intFile = intFile;
	}

	public String getErccH2hFile() {
		return erccH2hFile;
	}

	public void setErccH2hFile(String erccH2hFile) {
		this.erccH2hFile = erccH2hFile;
	}

	public String getErchH2hFile() {
		return erchH2hFile;
	}

	public void setErchH2hFile(String erchH2hFile) {
		this.erchH2hFile = erchH2hFile;
	}

	public String getUrcH2hFile() {
		return urcH2hFile;
	}

	public void setUrcH2hFile(String urcH2hFile) {
		this.urcH2hFile = urcH2hFile;
	}

	public String getPremH2hFile() {
		return premH2hFile;
	}

	public void setPremH2hFile(String premH2hFile) {
		this.premH2hFile = premH2hFile;
	}
}
