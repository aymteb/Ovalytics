package com.ovalytics.backend.domain;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "match_events",
		uniqueConstraints = @UniqueConstraint(columnNames = { "match_id", "external_id" }))
public class MatchEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "match_id", nullable = false)
	private RugbyMatch match;

	@Column(name = "external_id", nullable = false, length = 64)
	private String externalId;

	@Column(nullable = false, length = 40)
	private String periodLabel;

	@Column(nullable = false, length = 16)
	private String minuteLabel;

	@Column(nullable = false, length = 8)
	private String teamSide;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MatchEventType eventType;

	@Column(nullable = false, length = 120)
	private String playerName;

	@Column(nullable = false)
	private int sortOrder;

	protected MatchEvent() {
	}

	public MatchEvent(
			RugbyMatch match,
			String externalId,
			String periodLabel,
			String minuteLabel,
			String teamSide,
			MatchEventType eventType,
			String playerName,
			int sortOrder) {
		this.match = match;
		this.externalId = externalId;
		this.periodLabel = periodLabel;
		this.minuteLabel = minuteLabel;
		this.teamSide = teamSide;
		this.eventType = eventType;
		this.playerName = playerName;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public RugbyMatch getMatch() {
		return match;
	}

	public String getExternalId() {
		return externalId;
	}

	public String getPeriodLabel() {
		return periodLabel;
	}

	public String getMinuteLabel() {
		return minuteLabel;
	}

	public String getTeamSide() {
		return teamSide;
	}

	public MatchEventType getEventType() {
		return eventType;
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
