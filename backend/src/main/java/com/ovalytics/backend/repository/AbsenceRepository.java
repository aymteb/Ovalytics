package com.ovalytics.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.Absence;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

	@Query("""
			select a from Absence a
			join fetch a.player p
			where p.team.id = :teamId
			order by p.name asc
			""")
	List<Absence> findByTeamId(@Param("teamId") Long teamId);

	@Query("""
			select a from Absence a
			where a.player.id = :playerId
			""")
	List<Absence> findByPlayerId(@Param("playerId") Long playerId);
}
