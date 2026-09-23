package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.service.NewsQueryService;
import com.ovalytics.backend.web.dto.NewsItemResponse;

@RestController
@RequestMapping("/api/news")
public class NewsController {

	private final NewsQueryService newsQueryService;

	public NewsController(NewsQueryService newsQueryService) {
		this.newsQueryService = newsQueryService;
	}

	@GetMapping
	public List<NewsItemResponse> list(@RequestParam(defaultValue = "50") int limit) {
		return newsQueryService.listRecent(limit);
	}

	@GetMapping("/{id}")
	public NewsItemResponse one(@PathVariable Long id) {
		return newsQueryService.getById(id);
	}
}
