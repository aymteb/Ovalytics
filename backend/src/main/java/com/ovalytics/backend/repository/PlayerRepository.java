package com.ovalytics.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	@Query("""
			select p from Player p
			join fetch p.team t
			join fetch t.competition
			where p.id = :id
			""")
	Optional<Player> findByIdWithTeam(@Param("id") Long id);

	Optional<Player> findFirstByName(String name);

	Optional<Player> findByTeamIdAndName(Long teamId, String name);

	Optional<Player> findByTeamIdAndNameIgnoreCase(Long teamId, String name);

	List<Player> findByTeamIdOrderByNameAsc(Long teamId);

	@Query("""
			select count(p) from Player p
			join p.team t
			where t.competition.code = :code
			""")
	long countByCompetitionCode(@Param("code") String code);
}
