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
import com.ovalytics.backend.service.PendingTeamRefreshService;

@Component
public class MatchImportProcessor implements ItemProcessor<MatchCsvRow, RugbyMatch> {

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final PendingTeamRefreshService pendingTeamRefreshService;

	public MatchImportProcessor(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository,
			PendingTeamRefreshService pendingTeamRefreshService) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.pendingTeamRefreshService = pendingTeamRefreshService;
	}

	@Override
	public RugbyMatch process(MatchCsvRow row) {
		Integer homeScore = parseInt(row.homeScore());
		Integer awayScore = parseInt(row.awayScore());
		Integer homeTries = parseInt(row.homeTries());
		Integer awayTries = parseInt(row.awayTries());
		MatchStatus status = MatchStatus.valueOf(row.status());
		LocalDateTime kickoffAt = LocalDateTime.parse(row.kickoffAt());

		return findExisting(row, kickoffAt)
				.map(existing -> {
					MatchStatus previousStatus = existing.getStatus();
					existing.setKickoffAt(kickoffAt);
					existing.setStatus(status);
					existing.setHomeScore(homeScore);
					existing.setAwayScore(awayScore);
					existing.setHomeTries(homeTries);
					existing.setAwayTries(awayTries);
					enqueueIfFinished(previousStatus, existing);
					return existing;
				})
				.orElseGet(() -> {
					RugbyMatch created = createMatch(
							row, kickoffAt, status, homeScore, awayScore, homeTries, awayTries);
					enqueueIfFinished(MatchStatus.SCHEDULED, created);
					return created;
				});
	}

	private java.util.Optional<RugbyMatch> findExisting(MatchCsvRow row, LocalDateTime kickoffAt) {
		var byMatchday = rugbyMatchRepository.findByCompetitionAndTeamsAndMatchday(
				row.competitionCode(),
				row.homeShortName(),
				row.awayShortName(),
				row.matchday());
		if (byMatchday.isPresent()) {
			return byMatchday;
		}
		var dayStart = kickoffAt.toLocalDate().atStartOfDay();
		var dayEnd = dayStart.plusDays(1);
		return rugbyMatchRepository.findByTeamsOnDate(
				row.competitionCode(),
				row.homeShortName(),
				row.awayShortName(),
				dayStart,
				dayEnd);
	}

	private void enqueueIfFinished(MatchStatus previousStatus, RugbyMatch match) {
		if (previousStatus == MatchStatus.FINISHED || match.getStatus() != MatchStatus.FINISHED) {
			return;
		}
		if (match.getKickoffAt().isBefore(LocalDateTime.now().minusDays(14))) {
			return;
		}
		pendingTeamRefreshService.enqueue(match.getHomeTeam());
		pendingTeamRefreshService.enqueue(match.getAwayTeam());
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
