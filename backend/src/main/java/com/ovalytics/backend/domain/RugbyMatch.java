package com.ovalytics.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class RugbyMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "competition_id", nullable = false)
	private Competition competition;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "home_team_id", nullable = false)
	private Team homeTeam;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "away_team_id", nullable = false)
	private Team awayTeam;

	@Column(nullable = false)
	private LocalDateTime kickoffAt;

	@Column(nullable = false)
	private int matchday;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MatchStatus status;

	private Integer homeScore;

	private Integer awayScore;

	protected RugbyMatch() {
	}

	public RugbyMatch(
			Competition competition,
			Team homeTeam,
			Team awayTeam,
			LocalDateTime kickoffAt,
			int matchday,
			MatchStatus status,
			Integer homeScore,
			Integer awayScore) {
		this.competition = competition;
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
		this.kickoffAt = kickoffAt;
		this.matchday = matchday;
		this.status = status;
		this.homeScore = homeScore;
		this.awayScore = awayScore;
	}

	public Long getId() {
		return id;
	}

	public Competition getCompetition() {
		return competition;
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public LocalDateTime getKickoffAt() {
		return kickoffAt;
	}

	public int getMatchday() {
		return matchday;
	}

	public MatchStatus getStatus() {
		return status;
	}

	public Integer getHomeScore() {
		return homeScore;
	}

	public Integer getAwayScore() {
		return awayScore;
	}
}
