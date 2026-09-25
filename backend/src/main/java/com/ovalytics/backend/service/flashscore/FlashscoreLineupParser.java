package com.ovalytics.backend.service.flashscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FlashscoreLineupParser {

	private static final Pattern FIELD_PATTERN = Pattern.compile("([A-Z]{2,3})÷([^¬]*)");

	private FlashscoreLineupParser() {
	}

	public record LineupPlayer(
			String teamSide,
			int jerseyNumber,
			Integer position,
			String playerName,
			boolean starter,
			boolean captain) {
	}

	public static List<LineupPlayer> parse(String raw) {
		List<LineupPlayer> rows = new ArrayList<>();
		if (raw == null || raw.isBlank() || "0".equals(raw.trim())) {
			return rows;
		}

		String teamSide = "HOME";
		boolean starter = true;
		for (String chunk : raw.split("¬~")) {
			Map<String, String> fields = parseChunk(chunk);
			if (fields.containsKey("LB")) {
				String label = fields.getOrDefault("LB", "").toLowerCase();
				starter = !label.contains("substitut");
			}
			if (fields.containsKey("LC") && !fields.containsKey("LI")) {
				teamSide = "1".equals(fields.get("LC")) ? "HOME" : "AWAY";
				continue;
			}
			if (!fields.containsKey("LI")) {
				continue;
			}
			String name = fields.getOrDefault("LI", "").trim();
			if (name.isBlank()) {
				continue;
			}
			int jersey = parseInt(fields.get("LJ"), 0);
			if (jersey <= 0) {
				continue;
			}
			boolean isStarter = starter;
			if (fields.containsKey("LK")) {
				isStarter = "1".equals(fields.get("LK"));
			}
			Integer position = isStarter && jersey <= 15 ? jersey : null;
			rows.add(new LineupPlayer(teamSide, jersey, position, name, isStarter, false));
		}
		return rows;
	}

	private static int parseInt(String value, int fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	private static Map<String, String> parseChunk(String chunk) {
		Map<String, String> fields = new HashMap<>();
		Matcher matcher = FIELD_PATTERN.matcher(chunk);
		while (matcher.find()) {
			fields.put(matcher.group(1), matcher.group(2));
		}
		return fields;
	}
}
