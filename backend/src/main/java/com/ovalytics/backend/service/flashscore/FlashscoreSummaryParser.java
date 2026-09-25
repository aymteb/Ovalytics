package com.ovalytics.backend.service.flashscore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ovalytics.backend.domain.MatchEventType;

public final class FlashscoreSummaryParser {

	private static final Pattern FIELD_PATTERN = Pattern.compile("([A-Z]{2,3})÷([^¬]*)");

	private FlashscoreSummaryParser() {
	}

	public record SummaryEvent(
			String externalId,
			String periodLabel,
			String minuteLabel,
			String teamSide,
			MatchEventType eventType,
			String playerName,
			int sortOrder) {
	}

	public static List<SummaryEvent> parse(String raw) {
		List<SummaryEvent> events = new ArrayList<>();
		if (raw == null || raw.isBlank() || "0".equals(raw.trim())) {
			return events;
		}

		String period = "";
		int order = 0;
		for (String chunk : raw.split("¬~")) {
			Map<String, String> fields = parseChunk(chunk);
			if (fields.containsKey("AC") && !fields.containsKey("III")) {
				period = fields.getOrDefault("AC", period);
				continue;
			}
			if (!fields.containsKey("III") || !fields.containsKey("IF")) {
				continue;
			}
			String side = "1".equals(fields.get("IA")) ? "HOME" : "AWAY";
			events.add(new SummaryEvent(
					fields.get("III"),
					period.isBlank() ? "Match" : period,
					fields.getOrDefault("IB", ""),
					side,
					mapType(fields.getOrDefault("IK", "")),
					fields.getOrDefault("IF", "").trim(),
					order++));
		}
		return events;
	}

	private static MatchEventType mapType(String label) {
		String lower = label.toLowerCase();
		if (lower.contains("yellow")) {
			return MatchEventType.YELLOW;
		}
		if (lower.contains("red")) {
			return MatchEventType.RED;
		}
		if (lower.contains("try") || lower.contains("essai")) {
			return MatchEventType.TRY;
		}
		if (lower.contains("conversion")) {
			return MatchEventType.CONVERSION;
		}
		if (lower.contains("penalty") || lower.contains("pénal") || lower.contains("penal")) {
			return MatchEventType.PENALTY;
		}
		if (lower.contains("drop")) {
			return MatchEventType.DROP;
		}
		return MatchEventType.OTHER;
	}

	private static java.util.Map<String, String> parseChunk(String chunk) {
		java.util.HashMap<String, String> fields = new java.util.HashMap<>();
		Matcher matcher = FIELD_PATTERN.matcher(chunk);
		while (matcher.find()) {
			fields.put(matcher.group(1), matcher.group(2));
		}
		return fields;
	}
}
