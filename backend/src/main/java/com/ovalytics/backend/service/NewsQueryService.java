package com.ovalytics.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
		int size = Math.min(Math.max(limit, 1), 100);
		return newsItemRepository.findTop100ByOrderByPublishedAtDesc().stream()
				.limit(size)
				.map(this::toResponse)
				.toList();
	}

	public NewsItemResponse getById(Long id) {
		return newsItemRepository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "News item not found: " + id));
	}

	private NewsItemResponse toResponse(NewsItem item) {
		return new NewsItemResponse(
				item.getId(),
				item.getTitle(),
				item.getSummary(),
				item.getBody(),
				item.getImageUrl(),
				item.getSourceUrl(),
				item.getPublishedAt(),
				item.getSource(),
				item.getCompetitionCode());
	}
}
