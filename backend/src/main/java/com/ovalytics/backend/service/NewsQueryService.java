package com.ovalytics.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ovalytics.backend.domain.NewsItem;
import com.ovalytics.backend.repository.NewsItemRepository;
import com.ovalytics.backend.web.dto.NewsItemResponse;

@Service
public class NewsQueryService {

	private final NewsItemRepository newsItemRepository;

	public NewsQueryService(NewsItemRepository newsItemRepository) {
		this.newsItemRepository = newsItemRepository;
	}

	public List<NewsItemResponse> listRecent(int limit) {
		int size = Math.min(Math.max(limit, 1), 50);
		return newsItemRepository.findTop50ByOrderByPublishedAtDesc().stream()
				.limit(size)
				.map(this::toResponse)
				.toList();
	}

	private NewsItemResponse toResponse(NewsItem item) {
		return new NewsItemResponse(
				item.getId(),
				item.getTitle(),
				item.getSummary(),
				item.getSourceUrl(),
				item.getPublishedAt(),
				item.getSource(),
				item.getCompetitionCode());
	}
}
