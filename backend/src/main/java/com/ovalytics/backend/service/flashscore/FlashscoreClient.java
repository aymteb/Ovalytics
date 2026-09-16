package com.ovalytics.backend.service.flashscore;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ovalytics.backend.config.LiveScoreProperties;
import com.ovalytics.backend.domain.MatchStatus;

@Component
public class FlashscoreClient {

	private static final String FEED_BASE = "https://global.flashscore.ninja/2/x/feed";
	private static final String HUB_URL = "https://www.flashscore.fr/rugby/france/top-14/";
	private static final Pattern FEED_SIGN_PATTERN = Pattern.compile("\"feed_sign\":\"([^\"]+)\"");

	private final RestClient restClient;
	private final FlashscoreTeamMapper teamMapper;

	public FlashscoreClient(LiveScoreProperties properties, FlashscoreTeamMapper teamMapper) {
		this.teamMapper = teamMapper;
		this.restClient = RestClient.builder()
				.defaultHeader("User-Agent", properties.getUserAgent())
				.defaultHeader("Referer", "https://www.flashscore.fr/")
				.build();
	}

	public List<FlashscoreMatchUpdate> fetchLiveUpdates() {
		String sign = fetchFeedSign();
		Map<String, Map<String, String>> merged = new LinkedHashMap<>();

		for (int day = -1; day <= 1; day++) {
			String feedId = "f_8_" + day + "_3_fr_1";
			String raw = fetchFeed(feedId, sign);
			for (Map<String, String> fields : FlashscoreEventParser.parseFeed(raw)) {
				String eventId = fields.get("AA");
				if (eventId == null || eventId.isBlank()) {
					continue;
				}
				Map<String, String> existing = merged.get(eventId);
				if (existing == null || isFinished(fields)) {
					merged.put(eventId, fields);
				}
			}
		}

		String liveRaw = fetchFeed("u_8_1", sign);
		for (Map<String, String> fields : FlashscoreEventParser.parseFeed(liveRaw)) {
			String eventId = fields.get("AA");
			if (eventId == null || eventId.isBlank()) {
				continue;
			}
			Map<String, String> existing = merged.get(eventId);
			if (existing == null || isFinished(fields) || isLive(fields)) {
				merged.put(eventId, fields);
			}
		}

		return toUpdates(merged.values());
	}

	private List<FlashscoreMatchUpdate> toUpdates(Iterable<Map<String, String>> events) {
		List<FlashscoreMatchUpdate> updates = new ArrayList<>();
		for (Map<String, String> fields : events) {
			String homeName = fields.get("AE");
			String awayName = fields.get("AF");
			var pair = teamMapper.mapPair(homeName, awayName);
			if (pair.isEmpty()) {
				continue;
			}
			FlashscoreTeamMapper.TeamPair teams = pair.get();
			FlashscoreMatchUpdate update = FlashscoreMatchUpdate.fromFields(
					fields,
					teams.competitionCode(),
					teams.homeShortName(),
					teams.awayShortName());
			if (update != null && update.status() != MatchStatus.SCHEDULED) {
				updates.add(update);
			}
		}
		return updates;
	}

	private boolean isFinished(Map<String, String> fields) {
		return "3".equals(fields.get("AB"));
	}

	private boolean isLive(Map<String, String> fields) {
		String code = fields.getOrDefault("AB", "");
		return "2".equals(code) || "12".equals(code) || "13".equals(code);
	}

	private String fetchFeedSign() {
		String page = restClient.get()
				.uri(HUB_URL)
				.retrieve()
				.body(String.class);
		if (page == null) {
			throw new IllegalStateException("Flashscore hub vide");
		}
		Matcher matcher = FEED_SIGN_PATTERN.matcher(page);
		if (!matcher.find()) {
			throw new IllegalStateException("feed_sign introuvable sur Flashscore");
		}
		return matcher.group(1);
	}

	private String fetchFeed(String feedId, String sign) {
		try {
			return restClient.get()
					.uri(URI.create(FEED_BASE + "/" + feedId))
					.header("x-fsign", sign)
					.retrieve()
					.body(String.class);
		} catch (RuntimeException ex) {
			return "";
		}
	}
}
