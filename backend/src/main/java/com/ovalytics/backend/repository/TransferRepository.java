package com.ovalytics.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

	@Query("""
			select t from Transfer t
			join fetch t.competition
			left join fetch t.player
			left join fetch t.fromTeam
			left join fetch t.toTeam
			where t.competition.code = :code
			order by t.transferDate desc
			""")
	List<Transfer> findByCompetitionCodeOrderByTransferDateDesc(@Param("code") String code);

	@Query("""
			select t from Transfer t
			join fetch t.competition
			left join fetch t.player
			left join fetch t.fromTeam
			left join fetch t.toTeam
			order by t.transferDate desc, t.id desc
			""")
	List<Transfer> findAllByOrderByTransferDateDesc();

	@Query("""
			select t from Transfer t
			join fetch t.competition
			left join fetch t.player
			left join fetch t.fromTeam
			left join fetch t.toTeam
			where t.player.id = :playerId
			order by t.transferDate desc
			""")
	List<Transfer> findByPlayerIdOrderByTransferDateDesc(@Param("playerId") Long playerId);

	@Query("""
			select t from Transfer t
			join fetch t.competition
			left join fetch t.player
			left join fetch t.fromTeam
			left join fetch t.toTeam
			where lower(t.playerName) = lower(:name)
			order by t.transferDate desc, t.id desc
			""")
	List<Transfer> findByPlayerNameOrderByTransferDateDesc(@Param("name") String name);

	@Query("""
			select count(t) > 0
			from Transfer t
			where t.competition.code = :code
			""")
	boolean existsByCompetitionCode(@Param("code") String code);

	@Query("""
			select t from Transfer t
			where t.competition.code = :code
			  and t.playerName = :playerName
			  and t.transferDate = :transferDate
			  and t.type = :type
			  and coalesce(t.fromClubName, '') = :fromClub
			  and coalesce(t.toClubName, '') = :toClub
			""")
	Optional<Transfer> findByCompetitionCodeAndPlayerNameAndTransferDateAndTypeAndClubs(
			@Param("code") String code,
			@Param("playerName") String playerName,
			@Param("transferDate") LocalDate transferDate,
			@Param("type") TransferType type,
			@Param("fromClub") String fromClub,
			@Param("toClub") String toClub);

	@Query("""
			select t from Transfer t
			where t.competition.code = :code
			  and t.playerName = :playerName
			  and t.transferDate = :transferDate
			  and t.type = :type
			""")
	Optional<Transfer> findByCompetitionCodeAndPlayerNameAndTransferDateAndType(
			@Param("code") String code,
			@Param("playerName") String playerName,
			@Param("transferDate") LocalDate transferDate,
			@Param("type") TransferType type);
}
