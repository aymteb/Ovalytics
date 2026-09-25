package com.ovalytics.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.MatchEvent;
import com.ovalytics.backend.domain.MatchEventType;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

	List<MatchEvent> findByMatchIdOrderBySortOrderAsc(Long matchId);

	boolean existsByMatchId(Long matchId);

	void deleteByMatchId(Long matchId);

	long countByMatchId(Long matchId);

	@Modifying(clearAutomatically = true)
	@Query("delete from MatchEvent e where e.match.id = :matchId and e.eventType in :types")
	void deleteByMatchIdAndEventTypeIn(
			@Param("matchId") Long matchId,
			@Param("types") List<MatchEventType> types);
}
