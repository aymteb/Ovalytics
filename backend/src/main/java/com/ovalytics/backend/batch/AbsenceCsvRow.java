package com.ovalytics.backend.batch;

public record AbsenceCsvRow(
		String competitionCode,
		String teamShortName,
		String playerName,
		String type,
		String note) {
}
