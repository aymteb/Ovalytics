package com.ovalytics.backend.web.dto;

public record MatchEventResponse(
		String periodLabel,
		String minuteLabel,
		String teamSide,
		String eventType,
		String playerName) {
}
