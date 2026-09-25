package com.ovalytics.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ovalytics.backend.domain.MatchLineup;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {

	List<MatchLineup> findByMatchIdOrderByTeamSideAscStarterDescJerseyNumberAsc(Long matchId);

	boolean existsByMatchId(Long matchId);

	void deleteByMatchId(Long matchId);
}
