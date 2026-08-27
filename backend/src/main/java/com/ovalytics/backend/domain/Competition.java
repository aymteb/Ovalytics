package com.ovalytics.backend.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	private LocalDate seasonStart;

	@Column(nullable = false)
	private int defensiveBonusLimit;

	@Enumerated(EnumType.STRING)
	@Column(length = 40)
	private OffensiveBonusRule offensiveBonusRule;

	private Integer offensiveBonusThreshold;

	protected Competition() {
	}

	public Competition(
			String name,
			String code,
			String season,
			LocalDate seasonStart,
			int defensiveBonusLimit,
			OffensiveBonusRule offensiveBonusRule,
			int offensiveBonusThreshold) {
		this.name = name;
		this.code = code;
		this.season = season;
		this.seasonStart = seasonStart;
		this.defensiveBonusLimit = defensiveBonusLimit;
		this.offensiveBonusRule = offensiveBonusRule;
		this.offensiveBonusThreshold = offensiveBonusThreshold;
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

	public LocalDate getSeasonStart() {
		return seasonStart != null ? seasonStart : LocalDate.of(2025, 8, 1);
	}

	public void setSeasonStart(LocalDate seasonStart) {
		this.seasonStart = seasonStart;
	}

	public int getDefensiveBonusLimit() {
		return defensiveBonusLimit;
	}

	public OffensiveBonusRule getOffensiveBonusRule() {
		return offensiveBonusRule != null ? offensiveBonusRule : OffensiveBonusRule.TRY_DIFFERENCE;
	}

	public int getOffensiveBonusThreshold() {
		return offensiveBonusThreshold != null ? offensiveBonusThreshold : 3;
	}

	public void setOffensiveBonusRule(OffensiveBonusRule offensiveBonusRule) {
		this.offensiveBonusRule = offensiveBonusRule;
	}

	public void setOffensiveBonusThreshold(int offensiveBonusThreshold) {
		this.offensiveBonusThreshold = offensiveBonusThreshold;
	}
}
