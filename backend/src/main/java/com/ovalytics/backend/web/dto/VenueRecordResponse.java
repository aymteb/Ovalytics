package com.ovalytics.backend.web.dto;

public record VenueRecordResponse(
		int played,
		int won,
		int drawn,
		int lost) {
}
