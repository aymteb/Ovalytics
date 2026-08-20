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
@Table(name = "teams", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "competition_id", "short_name" })
})
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "short_name", nullable = false, length = 40)
	private String shortName;

	@Column(nullable = false, length = 80)
	private String city;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "competition_id", nullable = false)
	private Competition competition;

	protected Team() {
	}

	public Team(String name, String shortName, String city, Competition competition) {
		this.name = name;
		this.shortName = shortName;
		this.city = city;
		this.competition = competition;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getShortName() {
		return shortName;
	}

	public String getCity() {
		return city;
	}

	public Competition getCompetition() {
		return competition;
	}
}
