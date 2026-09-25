package com.ovalytics.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.config.LiveScoreProperties;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.service.flashscore.FlashscoreClient;
import com.ovalytics.backend.service.flashscore.FlashscoreMatchUpdate;

@Service
public class LiveScoreSyncService {

	private static final Logger log = LoggerFactory.getLogger(LiveScoreSyncService.class);

	private final LiveScoreProperties properties;
	private final FlashscoreClient flashscoreClient;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final PendingTeamRefreshService pendingTeamRefreshService;
	private final MatchSheetSyncService matchSheetSyncService;

	public LiveScoreSyncService(
			LiveScoreProperties properties,
			FlashscoreClient flashscoreClient,
			RugbyMatchRepository rugbyMatchRepository,
			PendingTeamRefreshService pendingTeamRefreshService,
			MatchSheetSyncService matchSheetSyncService) {
		this.properties = properties;
		this.flashscoreClient = flashscoreClient;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.pendingTeamRefreshService = pendingTeamRefreshService;
		this.matchSheetSyncService = matchSheetSyncService;
	}

	@Scheduled(fixedDelayString = "${ovalytics.live-scores.poll-interval-ms:60000}")
	@Transactional
	public void syncLiveScores() {
		if (!properties.isEnabled()) {
			return;
		}
		if (!shouldPoll()) {
			return;
		}

		List<FlashscoreMatchUpdate> updates;
		try {
			updates = flashscoreClient.fetchLiveUpdates();
		} catch (RuntimeException ex) {
			log.warn("Flashscore live indisponible: {}", ex.getMessage());
			return;
		}

		int applied = 0;
		for (FlashscoreMatchUpdate update : updates) {
			Optional<RugbyMatch> match = findMatch(update);
			boolean changed = false;
			if (match.isPresent()) {
				changed = applyUpdate(match.get(), update);
				if (changed) {
					applied++;
				}
				if (update.status() == MatchStatus.LIVE || update.status() == MatchStatus.FINISHED) {
					matchSheetSyncService.syncSheet(match.get());
				}
			}
		}
		if (applied > 0) {
			log.info("Live scores: {} match(s) mis a jour", applied);
		}
	}

	private boolean shouldPoll() {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime windowStart = now.minusHours(8);
		LocalDateTime windowEnd = now.plusHours(4);
		return rugbyMatchRepository.shouldPollLiveScores(
				MatchStatus.SCHEDULED,
				MatchStatus.LIVE,
				windowStart,
				windowEnd);
	}

	private boolean applyUpdate(RugbyMatch existing, FlashscoreMatchUpdate update) {
		MatchStatus previousStatus = existing.getStatus();
		boolean changed = false;

		if (update.flashscoreEventId() != null
				&& !update.flashscoreEventId().equals(existing.getFlashscoreEventId())) {
			existing.setFlashscoreEventId(update.flashscoreEventId());
			changed = true;
		}

		if (update.status() != existing.getStatus()) {
			existing.setStatus(update.status());
			changed = true;
		}

		if (update.hasScore()) {
			if (!update.homeScore().equals(existing.getHomeScore())) {
				existing.setHomeScore(update.homeScore());
				changed = true;
			}
			if (!update.awayScore().equals(existing.getAwayScore())) {
				existing.setAwayScore(update.awayScore());
				changed = true;
			}
		}

		if (!changed) {
			return false;
		}

		rugbyMatchRepository.save(existing);

		if (previousStatus != MatchStatus.FINISHED && existing.getStatus() == MatchStatus.FINISHED) {
			pendingTeamRefreshService.enqueue(existing.getHomeTeam());
			pendingTeamRefreshService.enqueue(existing.getAwayTeam());
		}

		return true;
	}

	private Optional<RugbyMatch> findMatch(FlashscoreMatchUpdate update) {
		if (update.flashscoreEventId() != null) {
			Optional<RugbyMatch> byEventId = rugbyMatchRepository
					.findByFlashscoreEventId(update.flashscoreEventId());
			if (byEventId.isPresent()) {
				return byEventId;
			}
		}

		if (update.matchday() != null && update.matchday() > 0) {
			Optional<RugbyMatch> byMatchday = rugbyMatchRepository.findByCompetitionAndTeamsAndMatchday(
					update.competitionCode(),
					update.homeShortName(),
					update.awayShortName(),
					update.matchday());
			if (byMatchday.isPresent()) {
				return byMatchday;
			}
		}

		LocalDate kickoffDate = update.kickoffAt().toLocalDate();
		LocalDateTime dayStart = kickoffDate.atStartOfDay();
		LocalDateTime dayEnd = dayStart.plusDays(1);
		return rugbyMatchRepository.findByTeamsOnDate(
				update.competitionCode(),
				update.homeShortName(),
				update.awayShortName(),
				dayStart,
				dayEnd);
	}
}
