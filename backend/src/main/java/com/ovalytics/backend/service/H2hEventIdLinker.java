package com.ovalytics.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.config.H2hSyncProperties;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.service.flashscore.FlashscoreClient;
import com.ovalytics.backend.service.flashscore.FlashscoreMatchUpdate;

@Service
public class H2hEventIdLinker {

	private static final Logger log = LoggerFactory.getLogger(H2hEventIdLinker.class);
	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	private final H2hSyncProperties properties;
	private final FlashscoreClient flashscoreClient;
	private final RugbyMatchRepository rugbyMatchRepository;

	public H2hEventIdLinker(
			H2hSyncProperties properties,
			FlashscoreClient flashscoreClient,
			RugbyMatchRepository rugbyMatchRepository) {
		this.properties = properties;
		this.flashscoreClient = flashscoreClient;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Transactional
	public int linkMissingEventIds() {
		List<FlashscoreMatchUpdate> updates = flashscoreClient.fetchScheduledCalendarUpdates();
		int linked = 0;
		for (FlashscoreMatchUpdate update : updates) {
			if (update.flashscoreEventId() == null || update.flashscoreEventId().isBlank()) {
				continue;
			}
			LocalDate day = update.kickoffAt().toLocalDate();
			LocalDateTime dayStart = day.atStartOfDay();
			LocalDateTime dayEnd = dayStart.plusDays(1);
			var match = rugbyMatchRepository.findByTeamsOnDate(
					update.competitionCode(),
					update.homeShortName(),
					update.awayShortName(),
					dayStart,
					dayEnd);
			if (match.isEmpty()) {
				continue;
			}
			RugbyMatch existing = match.get();
			if (update.flashscoreEventId().equals(existing.getFlashscoreEventId())) {
				continue;
			}
			existing.setFlashscoreEventId(update.flashscoreEventId());
			linked++;
		}
		log.info("H2H event ids rattaches: {}", linked);
		return linked;
	}

	@Transactional(readOnly = true)
	public List<String> collectEventIdsForUpcomingMatches() {
		LocalDateTime from = LocalDate.now(PARIS).atStartOfDay();
		LocalDateTime to = from.plusDays(Math.max(1, properties.getDaysAhead()));
		List<RugbyMatch> matches = rugbyMatchRepository.findByStatusAndKickoffBetween(
				MatchStatus.SCHEDULED, from, to);
		Set<String> ids = new LinkedHashSet<>();
		for (RugbyMatch match : matches) {
			String eventId = match.getFlashscoreEventId();
			if (eventId != null && !eventId.isBlank()) {
				ids.add(eventId);
			}
		}
		return new ArrayList<>(ids);
	}
}
