package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;

public record PlayerAppearanceResponse(
		Long matchId,
		LocalDateTime kickoffAt,
		int matchday,
		String competitionCode,
		String opponentShortName,
		String venue,
		String result,
		Integer homeScore,
		Integer awayScore,
		int jerseyNumber,
		boolean starter,
		int minutesPlayed,
		int tries,
		int yellowCards,
		int redCards) {
}
