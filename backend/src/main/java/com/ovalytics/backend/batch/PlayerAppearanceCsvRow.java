package com.ovalytics.backend.batch;

public record PlayerAppearanceCsvRow(
		String competitionCode,
		String teamShortName,
		String playerName,
		int matchday,
		String homeShortName,
		String awayShortName,
		String jerseyNumber,
		String minutesPlayed,
		String tries,
		String yellowCards,
		String redCards) {
}
