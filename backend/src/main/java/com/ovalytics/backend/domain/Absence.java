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

@Entity
@Table(name = "absences")
public class Absence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AbsenceType type;

	@Column(length = 200)
	private String note;

	protected Absence() {
	}

	public Absence(Player player, AbsenceType type, String note) {
		this.player = player;
		this.type = type;
		this.note = note;
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

	public AbsenceType getType() {
		return type;
	}

	public String getNote() {
		return note;
	}
}
