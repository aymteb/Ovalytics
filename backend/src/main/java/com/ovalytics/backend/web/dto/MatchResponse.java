package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchResponse(
		Long id,
		int matchday,
		LocalDateTime kickoffAt,
		String status,
		TeamResponse homeTeam,
		TeamResponse awayTeam,
		Integer homeScore,
		Integer awayScore,
		String analysis,
		List<AbsenceResponse> homeAbsences,
		List<AbsenceResponse> awayAbsences) {
}
