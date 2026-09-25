package com.ovalytics.backend.web.dto;

public record MatchLineupResponse(
		String teamSide,
		int jerseyNumber,
		Integer position,
		String playerName,
		boolean starter,
		boolean captain) {
}
