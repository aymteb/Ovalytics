package com.ovalytics.backend.batch;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.repository.NewsItemRepository;

@Component
public class NewsImportCleanupListener implements JobExecutionListener {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	private final NewsImportProperties properties;
	private final NewsItemRepository newsItemRepository;

	public NewsImportCleanupListener(
			NewsImportProperties properties,
			NewsItemRepository newsItemRepository) {
		this.properties = properties;
		this.newsItemRepository = newsItemRepository;
	}

	@Override
	@Transactional
	public void beforeJob(JobExecution jobExecution) {
		int retainDays = Math.max(1, properties.getRetainDays());
		LocalDateTime cutoff = LocalDateTime.now(PARIS).toLocalDate().atStartOfDay()
				.minusDays(retainDays - 1L);
		newsItemRepository.deleteByPublishedAtBefore(cutoff);
		newsItemRepository.deleteCompositionRecaps();
	}
}
