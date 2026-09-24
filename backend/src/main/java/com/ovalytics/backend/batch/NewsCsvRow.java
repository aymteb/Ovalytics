package com.ovalytics.backend.batch;

public record NewsCsvRow(
		String title,
		String summary,
		String body,
		String sourceUrl,
		String publishedAt,
		String source,
		String competitionCode,
		String imageUrl) {
}
