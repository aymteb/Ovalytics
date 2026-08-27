package com.ovalytics.backend.web.dto;

import java.time.LocalDate;

public record TransferResponse(
		Long id,
		LocalDate transferDate,
		String playerName,
		Long playerId,
		String type,
		String fromClub,
		String toClub,
		Long fromTeamId,
		Long toTeamId,
		String contractLength,
		String competitionCode,
		String competitionName) {
}
