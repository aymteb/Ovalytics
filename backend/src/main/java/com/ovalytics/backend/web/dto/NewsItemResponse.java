package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;

public record NewsItemResponse(
		Long id,
		String title,
		String summary,
		String imageUrl,
		String sourceUrl,
		LocalDateTime publishedAt,
		String source,
		String competitionCode) {
}
