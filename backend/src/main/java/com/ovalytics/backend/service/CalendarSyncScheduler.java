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
	private final Job nationaleMatchImportJob;
	private final Job erccMatchImportJob;
	private final Job erchMatchImportJob;
	private final Job urcMatchImportJob;
	private final Job premMatchImportJob;
	private final Job intMatchImportJob;

	public CalendarSyncScheduler(
			CalendarSyncProperties properties,
			CalendarScraperService calendarScraperService,
			JobOperator jobOperator,
			Job matchImportJob,
			Job prod2MatchImportJob,
			Job nationaleMatchImportJob,
			Job erccMatchImportJob,
			Job erchMatchImportJob,
			Job urcMatchImportJob,
			Job premMatchImportJob,
			Job intMatchImportJob) {
		this.properties = properties;
		this.calendarScraperService = calendarScraperService;
		this.jobOperator = jobOperator;
		this.matchImportJob = matchImportJob;
		this.prod2MatchImportJob = prod2MatchImportJob;
		this.nationaleMatchImportJob = nationaleMatchImportJob;
		this.erccMatchImportJob = erccMatchImportJob;
		this.erchMatchImportJob = erchMatchImportJob;
		this.urcMatchImportJob = urcMatchImportJob;
		this.premMatchImportJob = premMatchImportJob;
		this.intMatchImportJob = intMatchImportJob;
	}

	@Scheduled(cron = "${ovalytics.calendar-sync.cron:0 30 6 * * *}", zone = "Europe/Paris")
	public void syncCalendars() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync calendrier Top14 + Pro D2 + Nationale + etrangers");
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
			jobOperator.start(
					nationaleMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 2)
							.toJobParameters());
			jobOperator.start(
					erccMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 3)
							.toJobParameters());
			jobOperator.start(
					erchMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 4)
							.toJobParameters());
			jobOperator.start(
					urcMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 5)
							.toJobParameters());
			jobOperator.start(
					premMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 6)
							.toJobParameters());
			jobOperator.start(
					intMatchImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 7)
							.toJobParameters());
			log.info("Imports matchs calendrier lances");
		} catch (Exception ex) {
			log.error("Import calendrier impossible: {}", ex.getMessage());
		}
	}
}
