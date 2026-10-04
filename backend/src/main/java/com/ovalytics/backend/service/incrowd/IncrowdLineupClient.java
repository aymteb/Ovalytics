package com.ovalytics.backend.service.incrowd;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ovalytics.backend.config.LiveScoreProperties;

@Component
public class IncrowdLineupClient {

	private static final String FEED_BASE = "https://rugby-union-feeds.incrowdsports.com";
	private static final Pattern NON_LETTER = Pattern.compile("[^a-z0-9]+");
	private static final Map<String, Integer> COMPETITION_IDS = Map.of(
			"TOP14", 1002,
			"PROD2", 1013,
			"PREM", 1011,
			"URC", 1068,
			"ERCC", 1008,
			"ERCH", 1026);

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String apiKey;
	private final String realm;
	private final String appId;

	public IncrowdLineupClient(LiveScoreProperties properties) {
		this.apiKey = properties.getIncrowdApiKey();
		this.realm = properties.getIncrowdRealm();
		this.appId = properties.getIncrowdAppId();
		this.restClient = RestClient.builder()
				.defaultHeader("User-Agent", properties.getUserAgent())
				.defaultHeader("Accept", "application/json")
				.build();
	}

	public record LineupPlayer(
			String teamSide,
			int jerseyNumber,
			Integer position,
			String playerName,
			boolean starter,
			boolean captain) {
	}

	public List<LineupPlayer> fetchLineups(
			String competitionCode,
			LocalDateTime kickoffAt,
			String homeName,
			String awayName,
			String homeShortName,
			String awayShortName) {
		if (apiKey == null || apiKey.isBlank() || kickoffAt == null) {
			return List.of();
		}
		Integer competitionId = COMPETITION_IDS.get(competitionCode);
		if (competitionId == null) {
			return List.of();
		}
		LocalDate day = kickoffAt.toLocalDate();
		Optional<JsonNode> match = findMatch(
				competitionId,
				day.minusDays(1),
				day.plusDays(1),
				homeName,
				awayName,
				homeShortName,
				awayShortName);
		if (match.isEmpty()) {
			return List.of();
		}
		return fetchMatchLineups(match.get().path("id").asLong());
	}

	private Optional<JsonNode> findMatch(
			int competitionId,
			LocalDate from,
			LocalDate to,
			String homeName,
			String awayName,
			String homeShortName,
			String awayShortName) {
		String url = FEED_BASE + "/v1/matches?provider=rugbyviz"
				+ "&dateFrom=" + from
				+ "&dateTo=" + to
				+ "&pageSize=100";
		JsonNode root = getJson(url);
		if (root == null || !root.path("data").isArray()) {
			return Optional.empty();
		}
		for (JsonNode match : root.path("data")) {
			if (match.path("compId").asInt() != competitionId) {
				continue;
			}
			String feedHome = teamLabel(match.path("homeTeam"));
			String feedAway = teamLabel(match.path("awayTeam"));
			if (sameTeam(feedHome, homeName, homeShortName)
					&& sameTeam(feedAway, awayName, awayShortName)) {
				return Optional.of(match);
			}
		}
		return Optional.empty();
	}

	private List<LineupPlayer> fetchMatchLineups(long matchId) {
		String url = FEED_BASE + "/v1/matches/" + matchId + "?provider=rugbyviz";
		JsonNode root = getJson(url);
		if (root == null) {
			return List.of();
		}
		JsonNode data = root.path("data");
		List<LineupPlayer> rows = new ArrayList<>();
		rows.addAll(parseSide(data.path("homeTeam"), "HOME"));
		rows.addAll(parseSide(data.path("awayTeam"), "AWAY"));
		return rows;
	}

	private static List<LineupPlayer> parseSide(JsonNode team, String teamSide) {
		List<LineupPlayer> rows = new ArrayList<>();
		if (!team.path("players").isArray()) {
			return rows;
		}
		for (JsonNode player : team.path("players")) {
			int jersey = player.path("positionId").asInt(0);
			if (jersey <= 0) {
				continue;
			}
			String name = firstNonBlank(
					player.path("name").asText(null),
					player.path("known").asText(null),
					joinName(player));
			if (name == null || name.isBlank()) {
				continue;
			}
			boolean starter = jersey <= 15;
			boolean captain = player.path("captain").asBoolean(false);
			Integer position = starter ? jersey : null;
			rows.add(new LineupPlayer(teamSide, jersey, position, name.trim(), starter, captain));
		}
		return rows;
	}

	private JsonNode getJson(String url) {
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.header("X-API-KEY", apiKey)
					.header("X-REALM", realm)
					.header("X-APP-ID", appId)
					.retrieve()
					.body(String.class);
			if (body == null || body.isBlank()) {
				return null;
			}
			return objectMapper.readTree(body);
		} catch (Exception ex) {
			return null;
		}
	}

	private static String teamLabel(JsonNode team) {
		return firstNonBlank(
				team.path("name").asText(null),
				team.path("shortName").asText(null));
	}

	static boolean sameTeam(String feedName, String teamName, String shortName) {
		String feed = normalize(feedName);
		if (feed.isEmpty()) {
			return false;
		}
		String name = normalize(teamName);
		String shortCode = normalize(shortName);
		if (feed.equals(name) || (!name.isEmpty() && (feed.contains(name) || name.contains(feed)))) {
			return true;
		}
		if (!shortCode.isEmpty() && feed.contains(shortCode)) {
			return true;
		}
		for (String token : name.split(" ")) {
			if (token.length() >= 4 && feed.contains(token)) {
				return true;
			}
		}
		return false;
	}

	static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String lower = value.toLowerCase(Locale.ROOT);
		lower = lower
				.replace("é", "e").replace("è", "e").replace("ê", "e")
				.replace("à", "a").replace("â", "a")
				.replace("ô", "o").replace("î", "i").replace("ï", "i")
				.replace("ù", "u").replace("ç", "c");
		lower = lower
				.replace(" rugby", " ")
				.replace(" rfc", " ")
				.replace(" warriors", " ")
				.replace(" tigers", " ")
				.replace(" sharks", " ")
				.replace(" saints", " ")
				.replace(" chiefs", " ")
				.replace(" falcons", " ")
				.replace(" bears", " ")
				.replace(" red bulls", " ");
		Matcher matcher = NON_LETTER.matcher(lower);
		return matcher.replaceAll(" ").trim().replaceAll(" +", " ");
	}

	private static String joinName(JsonNode player) {
		String first = player.path("firstName").asText("").trim();
		String last = player.path("lastName").asText("").trim();
		String joined = (first + " " + last).trim();
		return joined.isEmpty() ? null : joined;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
