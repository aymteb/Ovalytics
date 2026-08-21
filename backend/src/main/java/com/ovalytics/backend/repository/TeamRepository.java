package com.ovalytics.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ovalytics.backend.domain.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

	List<Team> findByCompetitionCodeOrderByNameAsc(String competitionCode);

	Optional<Team> findByCompetitionCodeAndShortName(String competitionCode, String shortName);
}
