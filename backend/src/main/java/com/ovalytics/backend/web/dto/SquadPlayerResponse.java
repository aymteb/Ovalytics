package com.ovalytics.backend.web.dto;

import java.time.LocalDate;

public record SquadPlayerResponse(
		Long id,
		String name,
		String position,
		Integer age,
		Integer heightCm,
		Integer weightKg,
		String nationality,
		LocalDate contractEndDate) {
}
