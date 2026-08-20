package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;

public record MatchResponse(
		Long id,
		int matchday,
		LocalDateTime kickoffAt,
		String status,
		TeamResponse homeTeam,
		TeamResponse awayTeam,
		Integer homeScore,
		Integer awayScore) {
}
