package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.CalendarSyncProperties;

@Service
public class CalendarSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(CalendarSyncScheduler.class);

	private final CalendarSyncProperties properties;
	private final CalendarScraperService calendarScraperService;
	private final JobOperator jobOperator;
	private final Job matchImportJob;
	private final Job prod2MatchImportJob;

	public CalendarSyncScheduler(
			CalendarSyncProperties properties,
			CalendarScraperService calendarScraperService,
			JobOperator jobOperator,
			Job matchImportJob,
			Job prod2MatchImportJob) {
		this.properties = properties;
		this.calendarScraperService = calendarScraperService;
		this.jobOperator = jobOperator;
		this.matchImportJob = matchImportJob;
		this.prod2MatchImportJob = prod2MatchImportJob;
	}

	@Scheduled(cron = "${ovalytics.calendar-sync.cron:0 30 6 * * *}", zone = "Europe/Paris")
	public void syncCalendars() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync calendrier Top14 + Pro D2");
		boolean scraped = calendarScraperService.scrapeAll();
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape calendrier en echec, import ignore");
			return;
		}
		if (!scraped) {
			log.info("Scrape calendrier desactive, import des CSV existants");
		}

		try {
			long runId = System.currentTimeMillis();
			jobOperator.start(
					matchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId)
							.toJobParameters());
			jobOperator.start(
					prod2MatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 1)
							.toJobParameters());
			log.info("Imports matchs Top14 + Pro D2 lances");
		} catch (Exception ex) {
			log.error("Import calendrier impossible: {}", ex.getMessage());
		}
	}
}
