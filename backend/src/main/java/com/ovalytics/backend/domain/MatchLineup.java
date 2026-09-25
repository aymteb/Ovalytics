package com.ovalytics.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "match_lineups",
		uniqueConstraints = @UniqueConstraint(
				columnNames = { "match_id", "team_side", "jersey_number", "player_name" }))
public class MatchLineup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_id", nullable = false)
	private RugbyMatch match;

	@Column(name = "team_side", nullable = false, length = 8)
	private String teamSide;

	@Column(name = "jersey_number", nullable = false)
	private int jerseyNumber;

	@Column
	private Integer position;

	@Column(name = "player_name", nullable = false, length = 120)
	private String playerName;

	@Column(nullable = false)
	private boolean starter;

	@Column(nullable = false)
	private boolean captain;

	protected MatchLineup() {
	}

	public MatchLineup(
			RugbyMatch match,
			String teamSide,
			int jerseyNumber,
			Integer position,
			String playerName,
			boolean starter,
			boolean captain) {
		this.match = match;
		this.teamSide = teamSide;
		this.jerseyNumber = jerseyNumber;
		this.position = position;
		this.playerName = playerName;
		this.starter = starter;
		this.captain = captain;
	}

	public Long getId() {
		return id;
	}

	public RugbyMatch getMatch() {
		return match;
	}

	public String getTeamSide() {
		return teamSide;
	}

	public int getJerseyNumber() {
		return jerseyNumber;
	}

	public Integer getPosition() {
		return position;
	}

	public String getPlayerName() {
		return playerName;
	}

	public boolean isStarter() {
		return starter;
	}

	public boolean isCaptain() {
		return captain;
	}
}
