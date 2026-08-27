package com.ovalytics.backend.batch;

import java.time.LocalDateTime;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class MatchImportProcessor implements ItemProcessor<MatchCsvRow, RugbyMatch> {

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public MatchImportProcessor(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Override
	public RugbyMatch process(MatchCsvRow row) {
		Integer homeScore = parseInt(row.homeScore());
		Integer awayScore = parseInt(row.awayScore());
		Integer homeTries = parseInt(row.homeTries());
		Integer awayTries = parseInt(row.awayTries());
		MatchStatus status = MatchStatus.valueOf(row.status());
		LocalDateTime kickoffAt = LocalDateTime.parse(row.kickoffAt());

		return rugbyMatchRepository
				.findByCompetitionAndTeamsAndMatchday(
						row.competitionCode(),
						row.homeShortName(),
						row.awayShortName(),
						row.matchday())
				.map(existing -> {
					existing.setKickoffAt(kickoffAt);
					existing.setStatus(status);
					existing.setHomeScore(homeScore);
					existing.setAwayScore(awayScore);
					existing.setHomeTries(homeTries);
					existing.setAwayTries(awayTries);
					return existing;
				})
				.orElseGet(() -> createMatch(
						row, kickoffAt, status, homeScore, awayScore, homeTries, awayTries));
	}

	private RugbyMatch createMatch(
			MatchCsvRow row,
			LocalDateTime kickoffAt,
			MatchStatus status,
			Integer homeScore,
			Integer awayScore,
			Integer homeTries,
			Integer awayTries) {
		Competition competition = competitionRepository.findByCode(row.competitionCode())
				.orElseThrow(() -> new IllegalStateException(
						"Competition introuvable: " + row.competitionCode()));

		Team home = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.homeShortName())
				.orElseThrow(() -> new IllegalStateException(
						"Equipe introuvable: " + row.homeShortName()));

		Team away = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.awayShortName())
				.orElseThrow(() -> new IllegalStateException(
						"Equipe introuvable: " + row.awayShortName()));

		return new RugbyMatch(
				competition,
				home,
				away,
				kickoffAt,
				row.matchday(),
				status,
				homeScore,
				awayScore,
				homeTries,
				awayTries);
	}

	private static Integer parseInt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Integer.valueOf(value.trim());
	}
}
