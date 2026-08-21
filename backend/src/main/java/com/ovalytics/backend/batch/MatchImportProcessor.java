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
		if (rugbyMatchRepository.existsByCompetitionAndTeamsAndMatchday(
				row.competitionCode(),
				row.homeShortName(),
				row.awayShortName(),
				row.matchday())) {
			return null;
		}

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

		Integer homeScore = parseScore(row.homeScore());
		Integer awayScore = parseScore(row.awayScore());

		return new RugbyMatch(
				competition,
				home,
				away,
				LocalDateTime.parse(row.kickoffAt()),
				row.matchday(),
				MatchStatus.valueOf(row.status()),
				homeScore,
				awayScore);
	}

	private static Integer parseScore(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Integer.valueOf(value.trim());
	}
}
