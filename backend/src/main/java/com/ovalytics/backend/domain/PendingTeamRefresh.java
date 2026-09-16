package com.ovalytics.backend.domain;

import java.time.Instant;

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
@Table(name = "pending_team_refresh", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "team_id" })
})
public class PendingTeamRefresh {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@Column(nullable = false)
	private Instant createdAt;

	protected PendingTeamRefresh() {
	}

	public PendingTeamRefresh(Team team, Instant createdAt) {
		this.team = team;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Team getTeam() {
		return team;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
