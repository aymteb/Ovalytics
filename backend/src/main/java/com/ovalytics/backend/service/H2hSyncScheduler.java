package com.ovalytics.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.H2hSyncProperties;

@Service
public class H2hSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(H2hSyncScheduler.class);

	private final H2hSyncProperties properties;
	private final H2hEventIdLinker h2hEventIdLinker;
	private final H2hScraperService h2hScraperService;
	private final JobOperator jobOperator;
	private final Job matchH2hImportJob;
	private final Job prod2MatchH2hImportJob;
	private final Job nationaleMatchH2hImportJob;
	private final Job erccMatchH2hImportJob;
	private final Job erchMatchH2hImportJob;
	private final Job urcMatchH2hImportJob;
	private final Job premMatchH2hImportJob;

	public H2hSyncScheduler(
			H2hSyncProperties properties,
			H2hEventIdLinker h2hEventIdLinker,
			H2hScraperService h2hScraperService,
			JobOperator jobOperator,
			Job matchH2hImportJob,
			Job prod2MatchH2hImportJob,
			Job nationaleMatchH2hImportJob,
			Job erccMatchH2hImportJob,
			Job erchMatchH2hImportJob,
			Job urcMatchH2hImportJob,
			Job premMatchH2hImportJob) {
		this.properties = properties;
		this.h2hEventIdLinker = h2hEventIdLinker;
		this.h2hScraperService = h2hScraperService;
		this.jobOperator = jobOperator;
		this.matchH2hImportJob = matchH2hImportJob;
		this.prod2MatchH2hImportJob = prod2MatchH2hImportJob;
		this.nationaleMatchH2hImportJob = nationaleMatchH2hImportJob;
		this.erccMatchH2hImportJob = erccMatchH2hImportJob;
		this.erchMatchH2hImportJob = erchMatchH2hImportJob;
		this.urcMatchH2hImportJob = urcMatchH2hImportJob;
		this.premMatchH2hImportJob = premMatchH2hImportJob;
	}

	@Scheduled(cron = "${ovalytics.h2h-sync.cron:0 0 7 * * *}", zone = "Europe/Paris")
	public void syncH2h() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync H2H (event ids des matchs a venir)");
		int linked = h2hEventIdLinker.linkMissingEventIds();
		log.info("Event ids mis a jour: {}", linked);

		List<String> eventIds = h2hEventIdLinker.collectEventIdsForUpcomingMatches();
		boolean scraped = h2hScraperService.scrape(eventIds);
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape H2H en echec, import ignore");
			return;
		}
		if (!scraped) {
			log.info("Scrape H2H desactive, import des CSV existants");
		}

		try {
			long runId = System.currentTimeMillis();
			start(matchH2hImportJob, runId);
			start(prod2MatchH2hImportJob, runId + 1);
			start(nationaleMatchH2hImportJob, runId + 2);
			start(erccMatchH2hImportJob, runId + 3);
			start(erchMatchH2hImportJob, runId + 4);
			start(urcMatchH2hImportJob, runId + 5);
			start(premMatchH2hImportJob, runId + 6);
			log.info("Imports H2H lances");
		} catch (Exception ex) {
			log.error("Import H2H impossible: {}", ex.getMessage());
		}
	}

	private void start(Job job, long runId) throws Exception {
		jobOperator.start(
				job,
				new JobParametersBuilder()
						.addLong("run.id", runId)
						.toJobParameters());
	}
}
