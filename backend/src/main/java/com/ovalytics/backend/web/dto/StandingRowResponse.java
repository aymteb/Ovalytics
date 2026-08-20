package com.ovalytics.backend.web.dto;

public record StandingRowResponse(
		int position,
		Long teamId,
		String teamName,
		String teamShortName,
		int played,
		int won,
		int drawn,
		int lost,
		int pointsFor,
		int pointsAgainst,
		int pointsDifference,
		int bonus,
		int points) {
}
