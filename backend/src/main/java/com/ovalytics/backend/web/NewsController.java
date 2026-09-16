package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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
	public List<NewsItemResponse> list(@RequestParam(defaultValue = "20") int limit) {
		return newsQueryService.listRecent(limit);
	}
}
