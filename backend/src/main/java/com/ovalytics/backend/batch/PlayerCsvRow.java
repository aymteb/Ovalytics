package com.ovalytics.backend.batch;

public record PlayerCsvRow(
		String competitionCode,
		String teamShortName,
		String playerName,
		String position,
		String age,
		String heightCm,
		String weightKg,
		String nationality,
		String contractType,
		String jiffStatus,
		String contractEndDate) {
}
