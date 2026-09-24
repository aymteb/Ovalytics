package com.ovalytics.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ovalytics.backend.domain.NewsItem;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

	Optional<NewsItem> findBySourceUrl(String sourceUrl);

	List<NewsItem> findTop100ByOrderByPublishedAtDesc();

	List<NewsItem> findByPublishedAtGreaterThanEqual(LocalDateTime since);
}
