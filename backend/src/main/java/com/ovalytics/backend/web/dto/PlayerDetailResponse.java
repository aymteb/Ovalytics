package com.ovalytics.backend.web.dto;

import java.util.List;

public record PlayerDetailResponse(
		Long id,
		String name,
		TeamResponse team,
		String competitionCode,
		String competitionName,
		String season,
		String position,
		Integer age,
		Integer heightCm,
		Integer weightKg,
		String nationality,
		String jiffStatus,
		String photoUrl,
		PlayerTotalsResponse totals,
		List<PlayerAppearanceResponse> appearances,
		String careerHistory) {
}
