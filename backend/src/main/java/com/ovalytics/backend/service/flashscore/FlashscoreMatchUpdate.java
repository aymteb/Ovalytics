package com.ovalytics.backend.service.flashscore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.ovalytics.backend.domain.MatchStatus;

public record FlashscoreMatchUpdate(
		String competitionCode,
		String homeShortName,
		String awayShortName,
		Integer matchday,
		LocalDateTime kickoffAt,
		MatchStatus status,
		Integer homeScore,
		Integer awayScore,
		String flashscoreEventId) {

	public boolean hasScore() {
		return homeScore != null && awayScore != null;
	}

	static FlashscoreMatchUpdate fromFields(
			Map<String, String> fields,
			String competitionCode,
			String homeShort,
			String awayShort) {
		LocalDateTime kickoff = parseKickoff(fields.get("AD"));
		if (kickoff == null) {
			return null;
		}

		String statusCode = fields.getOrDefault("AB", "");
		MatchStatus status = mapStatus(statusCode, fields);
		Integer homeScore = parseScore(fields.get("AG"), status);
		Integer awayScore = parseScore(fields.get("AH"), status);
		Integer matchday = parseMatchday(fields.get("CR"));

		return new FlashscoreMatchUpdate(
				competitionCode,
				homeShort,
				awayShort,
				matchday,
				kickoff,
				status,
				homeScore,
				awayScore,
				fields.get("AA"));
	}

	private static MatchStatus mapStatus(String statusCode, Map<String, String> fields) {
		if ("3".equals(statusCode)) {
			return MatchStatus.FINISHED;
		}
		if ("2".equals(statusCode) || "12".equals(statusCode) || "13".equals(statusCode)) {
			return MatchStatus.LIVE;
		}
		String homeScore = fields.get("AG");
		String awayScore = fields.get("AH");
		if (homeScore != null && !homeScore.isBlank()
				&& awayScore != null && !awayScore.isBlank()
				&& !"0".equals(homeScore.concat(awayScore))) {
			return MatchStatus.LIVE;
		}
		return MatchStatus.SCHEDULED;
	}

	private static Integer parseScore(String value, MatchStatus status) {
		if (value == null || value.isBlank()) {
			return status == MatchStatus.FINISHED ? 0 : null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Integer parseMatchday(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static LocalDateTime parseKickoff(String timestamp) {
		if (timestamp == null || timestamp.isBlank() || !timestamp.chars().allMatch(Character::isDigit)) {
			return null;
		}
		Instant instant = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
