package com.ovalytics.backend.batch;

public record MatchCsvRow(
		String competitionCode,
		String homeShortName,
		String awayShortName,
		int matchday,
		String kickoffAt,
		String status,
		String homeScore,
		String awayScore) {
}
