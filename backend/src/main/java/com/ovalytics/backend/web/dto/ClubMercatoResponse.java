package com.ovalytics.backend.web.dto;

import java.util.List;

public record ClubMercatoResponse(
		TeamResponse team,
		String competitionCode,
		String competitionName,
		List<TransferResponse> arrivals,
		List<TransferResponse> departures,
		List<TransferResponse> extensions,
		int contractEndWatchYear,
		List<SquadPlayerResponse> contractEndsNextYear,
		List<SquadPlayerResponse> squad) {
}
