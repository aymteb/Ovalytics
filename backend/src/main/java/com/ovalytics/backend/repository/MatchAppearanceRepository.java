package com.ovalytics.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.MatchAppearance;

public interface MatchAppearanceRepository extends JpaRepository<MatchAppearance, Long> {

	@Query("""
			select a from MatchAppearance a
			join fetch a.match m
			join fetch m.homeTeam
			join fetch m.awayTeam
			join fetch m.competition
			where a.player.id = :playerId
			order by m.kickoffAt desc
			""")
	List<MatchAppearance> findByPlayerIdOrderByKickoffDesc(@Param("playerId") Long playerId);

	boolean existsByPlayerId(Long playerId);

	Optional<MatchAppearance> findByPlayerIdAndMatchId(Long playerId, Long matchId);
}
