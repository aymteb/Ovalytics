package com.ovalytics.backend.service.flashscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FlashscoreEventParser {

	private static final Pattern FIELD_PATTERN = Pattern.compile("([A-Z]{2})÷([^¬]*)");

	private FlashscoreEventParser() {
	}

	public static List<Map<String, String>> parseFeed(String raw) {
		List<Map<String, String>> events = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return events;
		}
		for (String chunk : raw.split("¬~")) {
			if (!chunk.contains("AA÷") || !chunk.contains("AE÷")) {
				continue;
			}
			Map<String, String> fields = parseChunk(chunk);
			if (!fields.containsKey("AA")) {
				continue;
			}
			events.add(fields);
		}
		return events;
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
