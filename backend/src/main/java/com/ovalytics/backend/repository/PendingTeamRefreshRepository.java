package com.ovalytics.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ovalytics.backend.domain.PendingTeamRefresh;

public interface PendingTeamRefreshRepository extends JpaRepository<PendingTeamRefresh, Long> {

	boolean existsByTeamId(Long teamId);

	@Query("""
			select p from PendingTeamRefresh p
			join fetch p.team t
			join fetch t.competition
			order by p.createdAt asc
			""")
	List<PendingTeamRefresh> findAllWithTeam();
}
