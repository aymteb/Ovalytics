package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;

public record HeadToHeadMatchResponse(
		Long id,
		LocalDateTime kickoffAt,
		String homeShortName,
		String awayShortName,
		Integer homeScore,
		Integer awayScore) {
}
