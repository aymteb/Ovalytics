package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.NewsSyncProperties;

@Service
public class NewsSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(NewsSyncScheduler.class);

	private final NewsSyncProperties properties;
	private final NewsScraperService newsScraperService;
	private final JobOperator jobOperator;
	private final Job newsImportJob;

	public NewsSyncScheduler(
			NewsSyncProperties properties,
			NewsScraperService newsScraperService,
			JobOperator jobOperator,
			Job newsImportJob) {
		this.properties = properties;
		this.newsScraperService = newsScraperService;
		this.jobOperator = jobOperator;
		this.newsImportJob = newsImportJob;
	}

	@Scheduled(cron = "${ovalytics.news-sync.cron:0 0 */2 * * *}", zone = "Europe/Paris")
	public void syncNews() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync actu multi-sources");
		boolean scraped = newsScraperService.scrape();
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape actu en echec, import ignore");
			return;
		}
		if (!scraped) {
			log.info("Scrape actu desactive, import du CSV existant");
		}

		try {
			jobOperator.start(
					newsImportJob,
					new JobParametersBuilder()
							.addLong("run.id", System.currentTimeMillis())
							.toJobParameters());
			log.info("Import actu lance");
		} catch (Exception ex) {
			log.error("Import actu impossible: {}", ex.getMessage());
		}
	}
}
