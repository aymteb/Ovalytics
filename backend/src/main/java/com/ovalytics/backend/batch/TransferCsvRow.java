package com.ovalytics.backend.batch;

public record TransferCsvRow(
		String competitionCode,
		String playerName,
		String type,
		String transferDate,
		String fromClub,
		String toClub,
		String contractLength) {
}
