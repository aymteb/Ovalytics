package com.ovalytics.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ovalytics.backend.domain.NewsItem;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

	Optional<NewsItem> findBySourceUrl(String sourceUrl);

	List<NewsItem> findTop100ByOrderByPublishedAtDesc();

	List<NewsItem> findByPublishedAtGreaterThanEqual(LocalDateTime since);

	void deleteByPublishedAtBefore(LocalDateTime cutoff);

	@Modifying
	@Query("""
			delete from NewsItem n
			where lower(n.title) like '%les compos pour%'
			   or lower(n.title) like '%les compos de %'
			   or lower(n.title) like '%les compos de la%'
			   or lower(n.title) like '%feuille de match%'
			""")
	int deleteCompositionRecaps();
}
