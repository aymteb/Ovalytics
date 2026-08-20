package com.ovalytics.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "competitions")
public class Competition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 20)
	private String season;

	@Column(nullable = false)
	private int defensiveBonusLimit;

	protected Competition() {
	}

	public Competition(String name, String code, String season, int defensiveBonusLimit) {
		this.name = name;
		this.code = code;
		this.season = season;
		this.defensiveBonusLimit = defensiveBonusLimit;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public String getSeason() {
		return season;
	}

	public int getDefensiveBonusLimit() {
		return defensiveBonusLimit;
	}
}
