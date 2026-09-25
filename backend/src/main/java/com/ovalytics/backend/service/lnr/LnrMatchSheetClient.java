package com.ovalytics.backend.service.lnr;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ovalytics.backend.config.LiveScoreProperties;
import com.ovalytics.backend.domain.MatchEventType;

@Component
public class LnrMatchSheetClient {

	private static final Pattern GAME_FACTS_PATTERN = Pattern.compile("game-facts='(\\[[^']*)'");
	private static final Pattern CARD_SUBTYPE_PATTERN = Pattern.compile(
			"\"slugSubType\"\\s*:\\s*\"([^\"]*(?:jaune|yellow|rouge|red)[^\"]*)\"",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PITCH_PATTERN = Pattern.compile(
			"<a[^>]*class=\"[^\"]*player-pitch([^\"]*)\"[\\s\\S]*?"
					+ "player-pitch__jersey\"[^>]*src=\"([^\"]*)\"[\\s\\S]*?"
					+ "player-pitch__number\">(\\d+)<[\\s\\S]*?"
					+ "player-pitch__first-name\">([^<]*)</span>\\s*"
					+ "<span class=\"player-pitch__last-name\">([^<]*)</span>",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern BENCH_PATTERN = Pattern.compile(
			"<[^>]*class=\"player-block([^\"]*lineup[^\"]*)\"[\\s\\S]*?"
					+ "player-block__number[^>]*>(\\d+)<[\\s\\S]*?"
					+ "player-block__name[^>]*>([^<]+)<",
			Pattern.CASE_INSENSITIVE);

	private final RestClient restClient;

	public LnrMatchSheetClient(LiveScoreProperties properties) {
		this.restClient = RestClient.builder()
				.defaultHeader("User-Agent", properties.getUserAgent())
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

	public record CardEvent(
			String externalId,
			String periodLabel,
			String minuteLabel,
			String teamSide,
			MatchEventType eventType,
			String playerName) {
	}

	public record SheetData(List<LineupPlayer> lineups, List<CardEvent> cards) {
	}

	public Optional<String> findFeuillePath(
			String competitionCode,
			int matchday,
			String homeShort,
			String awayShort) {
		String season = currentSeason();
		String host = hostFor(competitionCode);
		String week = "j" + matchday;
		String page = fetch(host + "/calendrier-et-resultats/" + season + "/" + week);
		if (page.isBlank()) {
			return Optional.empty();
		}
		String homeSlug = slugFor(homeShort);
		String awaySlug = slugFor(awayShort);
		if (homeSlug == null || awaySlug == null) {
			return Optional.empty();
		}
		Matcher matcher = Pattern.compile(
				"feuille-de-match/(" + Pattern.quote(season) + "/" + Pattern.quote(week)
						+ "/[0-9]+-[^\"'\\s>]+)")
				.matcher(page);
		while (matcher.find()) {
			String path = matcher.group(1).toLowerCase(Locale.ROOT);
			if (path.contains(homeSlug) && path.contains(awaySlug)) {
				return Optional.of(matcher.group(1));
			}
		}
		return Optional.empty();
	}

	public SheetData fetchSheet(String competitionCode, String feuillePath) {
		String host = hostFor(competitionCode);
		String base = host + "/feuille-de-match/" + feuillePath;
		String compositions = fetch(base + "/compositions");
		String factsPage = compositions.isBlank() ? fetch(base) : compositions;
		return new SheetData(parseLineups(compositions), parseCards(factsPage));
	}

	private List<LineupPlayer> parseLineups(String html) {
		List<LineupPlayer> rows = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return rows;
		}

		List<String> pitchSideOrder = new ArrayList<>();
		Matcher pitch = PITCH_PATTERN.matcher(html);
		while (pitch.find()) {
			String classes = pitch.group(1);
			String jerseySrc = pitch.group(2);
			int number = Integer.parseInt(pitch.group(3));
			String first = clean(pitch.group(4));
			String last = clean(pitch.group(5));
			Integer position = extractPosition(classes);
			boolean captain = classes.contains("captain");
			String side = sideFromJerseySrc(jerseySrc);
			if (pitchSideOrder.isEmpty() || !pitchSideOrder.get(pitchSideOrder.size() - 1).equals(side)) {
				if (!pitchSideOrder.contains(side)) {
					pitchSideOrder.add(side);
				}
			}
			rows.add(new LineupPlayer(
					side,
					number,
					position,
					formatName(first, last),
					true,
					captain));
		}

		if (pitchSideOrder.isEmpty()) {
			pitchSideOrder.add("HOME");
			pitchSideOrder.add("AWAY");
		} else if (pitchSideOrder.size() == 1) {
			pitchSideOrder.add(pitchSideOrder.get(0).equals("HOME") ? "AWAY" : "HOME");
		}

		Matcher bench = BENCH_PATTERN.matcher(html);
		int benchIndex = 0;
		while (bench.find()) {
			String classes = bench.group(1);
			if (classes.contains("player-pitch")) {
				continue;
			}
			int number = Integer.parseInt(bench.group(2));
			if (number < 16) {
				continue;
			}
			String name = formatListName(clean(bench.group(3)));
			boolean captain = classes.contains("captain");
			String side = sideForBench(pitchSideOrder, benchIndex++);
			rows.add(new LineupPlayer(side, number, null, name, false, captain));
		}
		return rows;
	}

	private static String sideFromJerseySrc(String jerseySrc) {
		String src = jerseySrc == null ? "" : jerseySrc.toLowerCase(Locale.ROOT);
		if (src.contains("away_jersey")) {
			return "AWAY";
		}
		return "HOME";
	}

	private static String sideForBench(List<String> pitchSideOrder, int index) {
		int mid = 8;
		if (index < mid) {
			return pitchSideOrder.get(0);
		}
		return pitchSideOrder.get(1);
	}

	private String formatListName(String raw) {
		String name = clean(raw);
		Matcher matcher = Pattern.compile(
				"^(.+?)\\s+([A-ZÀ-Ý][A-ZÀ-Ý'\\-]*(?:\\s+[A-ZÀ-Ý][A-ZÀ-Ý'\\-]*)*)$")
				.matcher(name);
		if (matcher.find()) {
			return formatName(matcher.group(1), matcher.group(2));
		}
		return name;
	}

	private List<CardEvent> parseCards(String html) {
		List<CardEvent> cards = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return cards;
		}
		Matcher matcher = GAME_FACTS_PATTERN.matcher(html);
		if (!matcher.find()) {
			return cards;
		}
		String factsJson = matcher.group(1).replace("&quot;", "\"");
		Matcher subtype = CARD_SUBTYPE_PATTERN.matcher(factsJson);
		while (subtype.find()) {
			MatchEventType type = mapCard(subtype.group(1));
			if (type == null) {
				continue;
			}
			String block = enclosingObject(factsJson, subtype.start());
			if (block.isBlank()) {
				continue;
			}
			String first = readString(block, "firstName");
			String last = readString(block, "lastName");
			String club = readString(block, "club");
			int minute = readInt(block, "minute", 0);
			int extra = readInt(block, "additionalMinute", 0);
			int period = readInt(block, "period", 1);
			String id = readString(block, "id");
			if (id.isBlank()) {
				int idNum = readInt(block, "id", -1);
				if (idNum >= 0) {
					id = String.valueOf(idNum);
				}
			}
			String minuteLabel = extra > 0 ? minute + "+" + extra + "'" : minute + "'";
			if (id.isBlank()) {
				id = minuteLabel + type;
			}
			cards.add(new CardEvent(
					id,
					period == 1 ? "1st Half" : "2nd Half",
					minuteLabel,
					"home".equalsIgnoreCase(club) ? "HOME" : "AWAY",
					type,
					formatName(first, last)));
		}
		return cards;
	}

	private static String enclosingObject(String json, int insideIndex) {
		int start = json.lastIndexOf('{', insideIndex);
		if (start < 0) {
			return "";
		}
		int depth = 0;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) {
					return json.substring(start, i + 1);
				}
			}
		}
		return "";
	}

	private static String readString(String block, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"")
				.matcher(block);
		return matcher.find() ? matcher.group(1) : "";
	}

	private static int readInt(String block, String field, int fallback) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)")
				.matcher(block);
		if (!matcher.find()) {
			return fallback;
		}
		return Integer.parseInt(matcher.group(1));
	}

	private MatchEventType mapCard(String subtype) {
		String lower = subtype.toLowerCase(Locale.ROOT);
		if (lower.contains("jaune") || lower.contains("yellow")) {
			return MatchEventType.YELLOW;
		}
		if (lower.contains("rouge") || lower.contains("red")) {
			return MatchEventType.RED;
		}
		return null;
	}

	private Integer extractPosition(String classes) {
		Matcher matcher = Pattern.compile("position-(\\d+)").matcher(classes);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		}
		return null;
	}

	private String formatName(String first, String last) {
		String f = clean(first);
		String l = clean(last);
		if (f.isBlank()) {
			return l;
		}
		if (l.isBlank()) {
			return f;
		}
		StringBuilder lastPretty = new StringBuilder();
		for (String part : l.split("\\s+")) {
			if (part.isBlank()) {
				continue;
			}
			if (lastPretty.length() > 0) {
				lastPretty.append(' ');
			}
			lastPretty.append(part.charAt(0));
			if (part.length() > 1) {
				lastPretty.append(part.substring(1).toLowerCase(Locale.ROOT));
			}
		}
		String initial = f.substring(0, 1).toUpperCase(Locale.ROOT);
		return lastPretty + " " + initial + ".";
	}

	private String clean(String value) {
		return value == null ? "" : value.replace('\u00a0', ' ').trim();
	}

	private String fetch(String url) {
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.header("Accept", "text/html,application/xhtml+xml")
					.retrieve()
					.body(String.class);
			return body == null ? "" : body;
		} catch (RuntimeException ex) {
			return "";
		}
	}

	private String hostFor(String competitionCode) {
		if ("TOP14".equals(competitionCode)) {
			return "https://top14.lnr.fr";
		}
		return "https://prod2.lnr.fr";
	}

	private String currentSeason() {
		return "2026-2027";
	}

	private String slugFor(String shortName) {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("TOU", "toulouse");
		map.put("RAC", "racing-92");
		map.put("SFP", "paris");
		map.put("TOL", "toulon");
		map.put("LAR", "la-rochelle");
		map.put("UBB", "bordeaux-begles");
		map.put("ASM", "clermont");
		map.put("LOU", "lyon");
		map.put("MHR", "montpellier");
		map.put("CAS", "castres");
		map.put("PAU", "pau");
		map.put("BAY", "bayonne");
		map.put("USAP", "perpignan");
		map.put("VAN", "vannes");
		map.put("BIA", "biarritz");
		map.put("NIC", "nice");
		map.put("ANG", "angouleme");
		map.put("COL", "colomiers");
		map.put("BEZ", "beziers");
		map.put("OYO", "oyonnax");
		map.put("DAX", "dax");
		map.put("NAR", "narbonne");
		map.put("GRE", "grenoble");
		map.put("AUR", "aurillac");
		map.put("NEV", "nevers");
		map.put("MTB", "montauban");
		map.put("AIX", "provence-rugby");
		map.put("AGE", "agen");
		map.put("BRI", "brive");
		map.put("VAL", "valence-romans");
		return map.get(shortName);
	}
}
