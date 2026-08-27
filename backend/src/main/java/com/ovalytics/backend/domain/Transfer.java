package com.ovalytics.backend.domain;

import java.time.LocalDate;

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
@Table(name = "transfers")
public class Transfer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "competition_id", nullable = false)
	private Competition competition;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id")
	private Player player;

	@Column(nullable = false, length = 120)
	private String playerName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferType type;

	@Column(nullable = false)
	private LocalDate transferDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_team_id")
	private Team fromTeam;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_team_id")
	private Team toTeam;

	@Column(length = 120)
	private String fromClubName;

	@Column(length = 120)
	private String toClubName;

	@Column(length = 40)
	private String contractLength;

	protected Transfer() {
	}

	public Transfer(
			Competition competition,
			Player player,
			String playerName,
			TransferType type,
			LocalDate transferDate,
			Team fromTeam,
			Team toTeam,
			String fromClubName,
			String toClubName,
			String contractLength) {
		this.competition = competition;
		this.player = player;
		this.playerName = playerName;
		this.type = type;
		this.transferDate = transferDate;
		this.fromTeam = fromTeam;
		this.toTeam = toTeam;
		this.fromClubName = fromClubName;
		this.toClubName = toClubName;
		this.contractLength = contractLength;
	}

	public Long getId() {
		return id;
	}

	public Competition getCompetition() {
		return competition;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public String getPlayerName() {
		return playerName;
	}

	public TransferType getType() {
		return type;
	}

	public LocalDate getTransferDate() {
		return transferDate;
	}

	public Team getFromTeam() {
		return fromTeam;
	}

	public Team getToTeam() {
		return toTeam;
	}

	public String getFromClubName() {
		return fromClubName;
	}

	public String getToClubName() {
		return toClubName;
	}

	public String getContractLength() {
		return contractLength;
	}
}
