package com.ovalytics.backend.web.dto;

import java.util.List;

public record PlayerDetailResponse(
		Long id,
		String name,
		TeamResponse team,
		String competitionCode,
		String competitionName,
		String position,
		Integer age,
		Integer heightCm,
		Integer weightKg,
		String nationality,
		PlayerTotalsResponse totals,
		List<PlayerAppearanceResponse> appearances,
		List<TransferResponse> transfers) {
}
