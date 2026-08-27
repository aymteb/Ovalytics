package com.ovalytics.backend.web.dto;

public record CompetitionResponse(
		Long id,
		String name,
		String code,
		String season) {
}
