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
	private static final List<String> RESULTS_URLS = List.of(
			"https://www.flashscore.fr/rugby/france/top-14/resultats/",
			"https://www.flashscore.fr/rugby/france/pro-d2/resultats/");
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

		return toUpdates(merged.values(), false);
	}

	public List<FlashscoreMatchUpdate> fetchRecentFinishedUpdates(int daysBack) {
		String sign = fetchFeedSign();
		Map<String, Map<String, String>> merged = new LinkedHashMap<>();
		int from = Math.max(1, daysBack);
		for (int day = -from; day <= 0; day++) {
			String raw = fetchFeed("f_8_" + day + "_3_fr_1", sign);
			mergeFinished(merged, FlashscoreEventParser.parseFeed(raw));
		}
		for (String resultsUrl : RESULTS_URLS) {
			String page = fetchPage(resultsUrl);
			mergeFinished(merged, FlashscoreEventParser.parseFeed(page));
		}
		return toUpdates(merged.values(), true);
	}

	private void mergeFinished(
			Map<String, Map<String, String>> merged,
			List<Map<String, String>> events) {
		for (Map<String, String> fields : events) {
			if (!isFinished(fields)) {
				continue;
			}
			String eventId = fields.get("AA");
			if (eventId == null || eventId.isBlank()) {
				continue;
			}
			merged.put(eventId, fields);
		}
	}

	public List<FlashscoreSummaryParser.SummaryEvent> fetchMatchSummary(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			return List.of();
		}
		String sign = fetchFeedSign();
		String raw = fetchFeed("df_sui_1_" + eventId, sign);
		return FlashscoreSummaryParser.parse(raw);
	}

	public List<FlashscoreLineupParser.LineupPlayer> fetchMatchLineups(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			return List.of();
		}
		String sign = fetchFeedSign();
		String raw = fetchFeed("df_li_1_" + eventId, sign);
		return FlashscoreLineupParser.parse(raw);
	}

	private List<FlashscoreMatchUpdate> toUpdates(
			Iterable<Map<String, String>> events,
			boolean includeFinishedOnly) {
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
			if (update == null) {
				continue;
			}
			if (includeFinishedOnly) {
				if (update.status() == MatchStatus.FINISHED) {
					updates.add(update);
				}
				continue;
			}
			if (update.status() != MatchStatus.SCHEDULED) {
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
		String page = fetchPage(HUB_URL);
		if (page == null || page.isBlank()) {
			throw new IllegalStateException("Flashscore hub vide");
		}
		Matcher matcher = FEED_SIGN_PATTERN.matcher(page);
		if (!matcher.find()) {
			throw new IllegalStateException("feed_sign introuvable sur Flashscore");
		}
		return matcher.group(1);
	}

	private String fetchPage(String url) {
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.retrieve()
					.body(String.class);
			return body == null ? "" : body;
		} catch (RuntimeException ex) {
			return "";
		}
	}

	private String fetchFeed(String feedId, String sign) {
		try {
			String body = restClient.get()
					.uri(URI.create(FEED_BASE + "/" + feedId))
					.header("x-fsign", sign)
					.retrieve()
					.body(String.class);
			return body == null ? "" : body;
		} catch (RuntimeException ex) {
			return "";
		}
	}
}
