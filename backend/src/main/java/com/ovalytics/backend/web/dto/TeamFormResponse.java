package com.ovalytics.backend.web.dto;

import java.util.List;

public record TeamFormResponse(
		List<String> results,
		int played,
		int won,
		int drawn,
		int lost,
		int fromPreviousSeason) {
}
