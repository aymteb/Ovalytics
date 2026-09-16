package com.ovalytics.backend.batch;

public record PlayerProfileCsvRow(
		String competitionCode,
		String teamShortName,
		String playerName,
		String profileUrl,
		String seasonMatches,
		String seasonStarts,
		String seasonMinutes,
		String seasonTries,
		String seasonYellowCards,
		String seasonRedCards,
		String contractEndDate,
		String careerHistory) {
}
