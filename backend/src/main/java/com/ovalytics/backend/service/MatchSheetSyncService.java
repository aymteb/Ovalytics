package com.ovalytics.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.config.LiveScoreProperties;
import com.ovalytics.backend.domain.MatchEvent;
import com.ovalytics.backend.domain.MatchEventType;
import com.ovalytics.backend.domain.MatchLineup;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.MatchEventRepository;
import com.ovalytics.backend.repository.MatchLineupRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.service.flashscore.FlashscoreClient;
import com.ovalytics.backend.service.flashscore.FlashscoreLineupParser;
import com.ovalytics.backend.service.flashscore.FlashscoreMatchUpdate;
import com.ovalytics.backend.service.flashscore.FlashscoreSummaryParser;
import com.ovalytics.backend.service.lnr.LnrMatchSheetClient;

@Service
public class MatchSheetSyncService {

	private static final Logger log = LoggerFactory.getLogger(MatchSheetSyncService.class);

	private final LiveScoreProperties properties;
	private final FlashscoreClient flashscoreClient;
	private final LnrMatchSheetClient lnrMatchSheetClient;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final MatchEventRepository matchEventRepository;
	private final MatchLineupRepository matchLineupRepository;
	private final MatchSheetSyncService self;

	public MatchSheetSyncService(
			LiveScoreProperties properties,
			FlashscoreClient flashscoreClient,
			LnrMatchSheetClient lnrMatchSheetClient,
			RugbyMatchRepository rugbyMatchRepository,
			MatchEventRepository matchEventRepository,
			MatchLineupRepository matchLineupRepository,
			@Lazy MatchSheetSyncService self) {
		this.properties = properties;
		this.flashscoreClient = flashscoreClient;
		this.lnrMatchSheetClient = lnrMatchSheetClient;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.matchEventRepository = matchEventRepository;
		this.matchLineupRepository = matchLineupRepository;
		this.self = self;
	}

	@Scheduled(cron = "0 20 * * * *", zone = "Europe/Paris")
	public void catchUpFinishedSheets() {
		if (!properties.isEnabled()) {
			return;
		}
		int synced = catchUp();
		if (synced > 0) {
			log.info("Feuilles de match: {} match(s) synchronise(s)", synced);
		}
	}

	public int catchUp() {
		int synced = syncMissingLnrLineups();
		List<FlashscoreMatchUpdate> updates;
		try {
			updates = flashscoreClient.fetchRecentFinishedUpdates(14);
		} catch (RuntimeException ex) {
			log.warn("Catch-up Flashscore impossible: {}", ex.getMessage());
			return synced;
		}
		for (FlashscoreMatchUpdate update : updates) {
			Optional<RugbyMatch> match;
			try {
				match = findMatch(update);
			} catch (RuntimeException ex) {
				log.warn(
						"Match Flashscore ignore ({}/{}): {}",
						update.homeShortName(),
						update.awayShortName(),
						ex.getMessage());
				continue;
			}
			if (match.isEmpty()) {
				continue;
			}
			try {
				if (self.syncFlashscoreAndSheet(match.get().getId(), update)) {
					synced++;
				}
			} catch (RuntimeException ex) {
				log.warn(
						"Feuille ignoree pour match {}: {}",
						match.get().getId(),
						ex.getMessage());
			}
		}
		return synced;
	}

	private int syncMissingLnrLineups() {
		int synced = 0;
		var since = java.time.LocalDateTime.now().minusDays(45);
		for (RugbyMatch match : rugbyMatchRepository.findByStatus(MatchStatus.FINISHED)) {
			if (match.getKickoffAt().isBefore(since)) {
				continue;
			}
			try {
				if (self.syncSheetById(match.getId())) {
					synced++;
				}
			} catch (RuntimeException ex) {
				log.warn(
						"Feuille ignoree pour match {}: {}",
						match.getId(),
						ex.getMessage());
			}
		}
		return synced;
	}

	@Transactional
	public boolean syncSheetById(Long matchId) {
		Optional<RugbyMatch> match = rugbyMatchRepository.findById(matchId);
		if (match.isEmpty()) {
			return false;
		}
		return syncSheet(match.get());
	}

	@Transactional
	public boolean syncFlashscoreAndSheet(Long matchId, FlashscoreMatchUpdate update) {
		RugbyMatch existing = rugbyMatchRepository.findById(matchId).orElse(null);
		if (existing == null) {
			return false;
		}
		boolean touched = false;
		if (update.flashscoreEventId() != null
				&& !update.flashscoreEventId().isBlank()
				&& !update.flashscoreEventId().equals(existing.getFlashscoreEventId())) {
			existing.setFlashscoreEventId(update.flashscoreEventId());
			touched = true;
		}
		if (update.hasScore()) {
			existing.setHomeScore(update.homeScore());
			existing.setAwayScore(update.awayScore());
			existing.setStatus(MatchStatus.FINISHED);
			touched = true;
		}
		if (touched) {
			rugbyMatchRepository.save(existing);
		}
		return syncSheet(existing);
	}

	@Transactional
	public boolean syncSheet(RugbyMatch match) {
		String eventId = match.getFlashscoreEventId();
		boolean flashOk = false;
		boolean flashLineupsOk = false;
		if (eventId != null && !eventId.isBlank()) {
			try {
				List<FlashscoreSummaryParser.SummaryEvent> summary =
						flashscoreClient.fetchMatchSummary(eventId);
				if (!summary.isEmpty()) {
					matchEventRepository.deleteByMatchId(match.getId());
					matchEventRepository.flush();
					List<MatchEvent> rows = new ArrayList<>();
					int homeTries = 0;
					int awayTries = 0;
					for (FlashscoreSummaryParser.SummaryEvent item : summary) {
						rows.add(new MatchEvent(
								match,
								item.externalId(),
								item.periodLabel(),
								item.minuteLabel(),
								item.teamSide(),
								item.eventType(),
								item.playerName(),
								item.sortOrder()));
						if (item.eventType() == MatchEventType.TRY) {
							if ("HOME".equals(item.teamSide())) {
								homeTries++;
							} else {
								awayTries++;
							}
						}
					}
					matchEventRepository.saveAll(rows);
					matchEventRepository.flush();
					match.setHomeTries(homeTries);
					match.setAwayTries(awayTries);
					rugbyMatchRepository.save(match);
					flashOk = true;
				}
			} catch (RuntimeException ex) {
				log.warn("Resume Flashscore indisponible pour {}: {}", eventId, ex.getMessage());
			}
			try {
				flashLineupsOk = saveFlashscoreLineups(match, eventId);
			} catch (RuntimeException ex) {
				log.warn("Compositions Flashscore indisponibles pour {}: {}", eventId, ex.getMessage());
			}
		}
		boolean lnrOk = syncLnrSheet(match, flashLineupsOk);
		return flashOk || flashLineupsOk || lnrOk;
	}

	private boolean saveFlashscoreLineups(RugbyMatch match, String eventId) {
		List<FlashscoreLineupParser.LineupPlayer> players =
				flashscoreClient.fetchMatchLineups(eventId);
		if (players.isEmpty()) {
			return false;
		}
		matchLineupRepository.deleteByMatchId(match.getId());
		matchLineupRepository.flush();
		List<MatchLineup> rows = new ArrayList<>();
		for (FlashscoreLineupParser.LineupPlayer player : players) {
			rows.add(new MatchLineup(
					match,
					player.teamSide(),
					player.jerseyNumber(),
					player.position(),
					player.playerName(),
					player.starter(),
					player.captain()));
		}
		matchLineupRepository.saveAll(rows);
		return true;
	}

	@Transactional
	public boolean syncLnrSheet(RugbyMatch match) {
		return syncLnrSheet(match, false);
	}

	@Transactional
	public boolean syncLnrSheet(RugbyMatch match, boolean keepExistingLineups) {
		String competitionCode = match.getCompetition().getCode();
		Optional<String> path = lnrMatchSheetClient.findFeuillePath(
				competitionCode,
				match.getMatchday(),
				match.getHomeTeam().getShortName(),
				match.getAwayTeam().getShortName());
		if (path.isEmpty()) {
			log.debug(
					"Feuille LNR introuvable pour {} {}-{} J{}",
					competitionCode,
					match.getHomeTeam().getShortName(),
					match.getAwayTeam().getShortName(),
					match.getMatchday());
			return false;
		}

		LnrMatchSheetClient.SheetData sheet =
				lnrMatchSheetClient.fetchSheet(competitionCode, path.get());
		boolean changed = false;

		if (!keepExistingLineups && !sheet.lineups().isEmpty()) {
			matchLineupRepository.deleteByMatchId(match.getId());
			matchLineupRepository.flush();
			List<MatchLineup> rows = new ArrayList<>();
			for (LnrMatchSheetClient.LineupPlayer player : sheet.lineups()) {
				rows.add(new MatchLineup(
						match,
						player.teamSide(),
						player.jerseyNumber(),
						player.position(),
						player.playerName(),
						player.starter(),
						player.captain()));
			}
			matchLineupRepository.saveAll(rows);
			changed = true;
		} else if (sheet.lineups().isEmpty()) {
			log.debug("Compositions LNR vides pour {}", path.get());
		}

		if (!sheet.cards().isEmpty()) {
			matchEventRepository.deleteByMatchIdAndEventTypeIn(
					match.getId(),
					List.of(MatchEventType.YELLOW, MatchEventType.RED));
			matchEventRepository.flush();
			int baseOrder = (int) matchEventRepository.countByMatchId(match.getId()) + 1;
			List<MatchEvent> cards = new ArrayList<>();
			int order = baseOrder;
			for (LnrMatchSheetClient.CardEvent card : sheet.cards()) {
				cards.add(new MatchEvent(
						match,
						"lnr-" + card.externalId(),
						card.periodLabel(),
						card.minuteLabel(),
						card.teamSide(),
						card.eventType(),
						card.playerName(),
						order++));
			}
			matchEventRepository.saveAll(cards);
			reorderEventsByMinute(match.getId());
			changed = true;
		}

		return changed;
	}

	private void reorderEventsByMinute(Long matchId) {
		List<MatchEvent> events = matchEventRepository.findByMatchIdOrderBySortOrderAsc(matchId);
		events.sort((a, b) -> {
			int byPeriod = periodRank(a.getPeriodLabel()) - periodRank(b.getPeriodLabel());
			if (byPeriod != 0) {
				return byPeriod;
			}
			int byMinute = minuteValue(a.getMinuteLabel()) - minuteValue(b.getMinuteLabel());
			if (byMinute != 0) {
				return byMinute;
			}
			return Integer.compare(a.getSortOrder(), b.getSortOrder());
		});
		int order = 1;
		for (MatchEvent event : events) {
			event.setSortOrder(order++);
		}
		matchEventRepository.saveAll(events);
	}

	private static int periodRank(String period) {
		if (period == null || period.isBlank()) {
			return 0;
		}
		String lower = period.toLowerCase(Locale.ROOT);
		if (lower.contains("2nd") || lower.contains("2e") || lower.contains("second")) {
			return 2;
		}
		return 1;
	}

	private static int minuteValue(String minuteLabel) {
		if (minuteLabel == null || minuteLabel.isBlank()) {
			return 0;
		}
		Matcher matcher = Pattern.compile("(\\d+)(?:\\+(\\d+))?").matcher(minuteLabel);
		if (!matcher.find()) {
			return 0;
		}
		int base = Integer.parseInt(matcher.group(1));
		int extra = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
		return base * 100 + extra;
	}

	private Optional<RugbyMatch> findMatch(FlashscoreMatchUpdate update) {
		if (update.flashscoreEventId() != null && !update.flashscoreEventId().isBlank()) {
			List<RugbyMatch> byEventId = rugbyMatchRepository
					.findAllByFlashscoreEventId(update.flashscoreEventId());
			if (!byEventId.isEmpty()) {
				return Optional.of(byEventId.get(0));
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
		var kickoffDate = update.kickoffAt().toLocalDate();
		return rugbyMatchRepository.findByTeamsOnDate(
				update.competitionCode(),
				update.homeShortName(),
				update.awayShortName(),
				kickoffDate.atStartOfDay(),
				kickoffDate.atStartOfDay().plusDays(1));
	}
}
