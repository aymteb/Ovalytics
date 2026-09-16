package com.ovalytics.backend.web.dto;

public record ClubJiffSummaryResponse(
		int jiffCount,
		int nonJiffCount,
		int nonJiffLimit) {
}
