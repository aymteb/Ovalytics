package com.ovalytics.backend.batch;

import java.time.LocalDateTime;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.NewsItem;
import com.ovalytics.backend.repository.NewsItemRepository;

@Component
public class NewsImportProcessor implements ItemProcessor<NewsCsvRow, NewsItem> {

	private final NewsItemRepository newsItemRepository;

	public NewsImportProcessor(NewsItemRepository newsItemRepository) {
		this.newsItemRepository = newsItemRepository;
	}

	@Override
	public NewsItem process(NewsCsvRow row) {
		LocalDateTime publishedAt = LocalDateTime.parse(row.publishedAt());
		String competitionCode = blankToNull(row.competitionCode());
		String imageUrl = blankToNull(row.imageUrl());

		return newsItemRepository.findBySourceUrl(row.sourceUrl())
				.map(existing -> {
					existing.setTitle(row.title());
					existing.setSummary(blankToNull(row.summary()));
					existing.setImageUrl(imageUrl);
					existing.setPublishedAt(publishedAt);
					existing.setCompetitionCode(competitionCode);
					return existing;
				})
				.orElseGet(() -> new NewsItem(
						row.title(),
						blankToNull(row.summary()),
						imageUrl,
						row.sourceUrl(),
						publishedAt,
						row.source(),
						competitionCode));
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
