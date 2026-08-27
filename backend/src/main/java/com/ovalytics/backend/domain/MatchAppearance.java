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
		name = "match_appearances",
		uniqueConstraints = @UniqueConstraint(columnNames = { "player_id", "match_id" }))
public class MatchAppearance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_id", nullable = false)
	private RugbyMatch match;

	@Column(nullable = false)
	private int jerseyNumber;

	@Column(nullable = false)
	private boolean starter;

	@Column(nullable = false)
	private int minutesPlayed;

	@Column(nullable = false)
	private int tries;

	@Column(nullable = false)
	private int yellowCards;

	@Column(nullable = false)
	private int redCards;

	protected MatchAppearance() {
	}

	public MatchAppearance(
			Player player,
			RugbyMatch match,
			int jerseyNumber,
			boolean starter,
			int minutesPlayed,
			int tries,
			int yellowCards,
			int redCards) {
		this.player = player;
		this.match = match;
		this.jerseyNumber = jerseyNumber;
		this.starter = starter;
		this.minutesPlayed = minutesPlayed;
		this.tries = tries;
		this.yellowCards = yellowCards;
		this.redCards = redCards;
	}

	public Long getId() {
		return id;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public RugbyMatch getMatch() {
		return match;
	}

	public int getJerseyNumber() {
		return jerseyNumber;
	}

	public boolean isStarter() {
		return starter;
	}

	public int getMinutesPlayed() {
		return minutesPlayed;
	}

	public int getTries() {
		return tries;
	}

	public int getYellowCards() {
		return yellowCards;
	}

	public int getRedCards() {
		return redCards;
	}
}
