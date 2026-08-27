package com.ovalytics.backend.web.dto;

public record PlayerTotalsResponse(
		int matches,
		int starts,
		int minutes,
		int tries,
		int yellowCards,
		int redCards) {
}
