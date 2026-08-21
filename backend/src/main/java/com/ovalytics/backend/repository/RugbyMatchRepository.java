package com.ovalytics.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;

public interface RugbyMatchRepository extends JpaRepository<RugbyMatch, Long> {

	@Query("""
			select m from RugbyMatch m
			join fetch m.homeTeam
			join fetch m.awayTeam
			join fetch m.competition
			where m.competition.code = :code
			order by m.kickoffAt asc
			""")
	List<RugbyMatch> findByCompetitionCode(@Param("code") String code);

	@Query("""
			select m from RugbyMatch m
			join fetch m.homeTeam
			join fetch m.awayTeam
			join fetch m.competition
			where m.competition.code = :code
			  and m.status = :status
			order by m.kickoffAt asc
			""")
	List<RugbyMatch> findByCompetitionCodeAndStatus(
			@Param("code") String code,
			@Param("status") MatchStatus status);

	@Query("""
			select m from RugbyMatch m
			join fetch m.homeTeam
			join fetch m.awayTeam
			join fetch m.competition
			where m.competition.code = :code
			  and m.id = :id
			""")
	Optional<RugbyMatch> findByCompetitionCodeAndId(
			@Param("code") String code,
			@Param("id") Long id);
}
