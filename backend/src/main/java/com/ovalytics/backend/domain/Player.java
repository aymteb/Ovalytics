package com.ovalytics.backend.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "players")
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@Column(length = 40)
	private String position;

	private Integer age;

	private Integer heightCm;

	private Integer weightKg;

	@Column(length = 60)
	private String nationality;

	@Column(length = 10)
	private String contractType;

	@Column(length = 20)
	private String jiffStatus;

	private LocalDate contractEndDate;

	@Column(length = 255)
	private String allRugbyProfileUrl;

	private Integer seasonMatches;

	private Integer seasonStarts;

	private Integer seasonMinutes;

	private Integer seasonTries;

	private Integer seasonYellowCards;

	private Integer seasonRedCards;

	@Column(length = 1000)
	private String careerHistory;

	protected Player() {
	}

	public Player(String name, Team team) {
		this(name, team, null, null, null, null, null, null);
	}

	public Player(
			String name,
			Team team,
			String position,
			Integer age,
			Integer heightCm,
			Integer weightKg,
			String nationality) {
		this(name, team, position, age, heightCm, weightKg, nationality, null);
	}

	public Player(
			String name,
			Team team,
			String position,
			Integer age,
			Integer heightCm,
			Integer weightKg,
			String nationality,
			LocalDate contractEndDate) {
		this.name = name;
		this.team = team;
		this.position = position;
		this.age = age;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.nationality = nationality;
		this.contractEndDate = contractEndDate;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public String getPosition() {
		return position;
	}

	public Integer getAge() {
		return age;
	}

	public Integer getHeightCm() {
		return heightCm;
	}

	public Integer getWeightKg() {
		return weightKg;
	}

	public String getNationality() {
		return nationality;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getJiffStatus() {
		return jiffStatus;
	}

	public void setJiffStatus(String jiffStatus) {
		this.jiffStatus = jiffStatus;
	}

	public LocalDate getContractEndDate() {
		return contractEndDate;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public void setHeightCm(Integer heightCm) {
		this.heightCm = heightCm;
	}

	public void setWeightKg(Integer weightKg) {
		this.weightKg = weightKg;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public void setContractEndDate(LocalDate contractEndDate) {
		this.contractEndDate = contractEndDate;
	}

	public String getAllRugbyProfileUrl() {
		return allRugbyProfileUrl;
	}

	public void setAllRugbyProfileUrl(String allRugbyProfileUrl) {
		this.allRugbyProfileUrl = allRugbyProfileUrl;
	}

	public Integer getSeasonMatches() {
		return seasonMatches;
	}

	public void setSeasonMatches(Integer seasonMatches) {
		this.seasonMatches = seasonMatches;
	}

	public Integer getSeasonStarts() {
		return seasonStarts;
	}

	public void setSeasonStarts(Integer seasonStarts) {
		this.seasonStarts = seasonStarts;
	}

	public Integer getSeasonMinutes() {
		return seasonMinutes;
	}

	public void setSeasonMinutes(Integer seasonMinutes) {
		this.seasonMinutes = seasonMinutes;
	}

	public Integer getSeasonTries() {
		return seasonTries;
	}

	public void setSeasonTries(Integer seasonTries) {
		this.seasonTries = seasonTries;
	}

	public Integer getSeasonYellowCards() {
		return seasonYellowCards;
	}

	public void setSeasonYellowCards(Integer seasonYellowCards) {
		this.seasonYellowCards = seasonYellowCards;
	}

	public Integer getSeasonRedCards() {
		return seasonRedCards;
	}

	public void setSeasonRedCards(Integer seasonRedCards) {
		this.seasonRedCards = seasonRedCards;
	}

	public String getCareerHistory() {
		return careerHistory;
	}

	public void setCareerHistory(String careerHistory) {
		this.careerHistory = careerHistory;
	}
}
