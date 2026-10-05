package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.TransferSyncProperties;

@Service
public class TransferSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(TransferSyncScheduler.class);

	private final TransferSyncProperties properties;
	private final TransferScraperService transferScraperService;
	private final JobOperator jobOperator;
	private final Job transferImportJob;

	public TransferSyncScheduler(
			TransferSyncProperties properties,
			TransferScraperService transferScraperService,
			JobOperator jobOperator,
			Job transferImportJob) {
		this.properties = properties;
		this.transferScraperService = transferScraperService;
		this.jobOperator = jobOperator;
		this.transferImportJob = transferImportJob;
	}

	@Scheduled(cron = "${ovalytics.transfer-sync.cron:0 0 12,20 * * *}", zone = "Europe/Paris")
	public void syncTransfers() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync transferts");
		boolean scraped = transferScraperService.scrape();
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape transferts en echec, import ignore");
			return;
		}
		if (!scraped) {
			log.info("Scrape transferts desactive, import du CSV existant");
		}

		try {
			jobOperator.start(
					transferImportJob,
					new JobParametersBuilder()
							.addLong("run.id", System.currentTimeMillis())
							.toJobParameters());
			log.info("Import transferts lance");
		} catch (Exception ex) {
			log.error("Import transferts impossible: {}", ex.getMessage());
		}
	}
}
