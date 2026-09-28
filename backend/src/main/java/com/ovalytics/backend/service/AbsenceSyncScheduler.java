package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.AbsenceSyncProperties;

@Service
public class AbsenceSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(AbsenceSyncScheduler.class);

	private final AbsenceSyncProperties properties;
	private final AbsenceScraperService absenceScraperService;
	private final JobOperator jobOperator;
	private final Job absenceImportJob;

	public AbsenceSyncScheduler(
			AbsenceSyncProperties properties,
			AbsenceScraperService absenceScraperService,
			JobOperator jobOperator,
			Job absenceImportJob) {
		this.properties = properties;
		this.absenceScraperService = absenceScraperService;
		this.jobOperator = jobOperator;
		this.absenceImportJob = absenceImportJob;
	}

	@Scheduled(cron = "${ovalytics.absence-sync.cron:0 0 8,18 * * *}", zone = "Europe/Paris")
	public void syncAbsences() {
		if (!properties.isEnabled()) {
			return;
		}
		runSync();
	}

	public void runSync() {
		log.info("Sync absences Top 14 + Pro D2");
		boolean scraped = absenceScraperService.scrape();
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape absences en echec, import ignore");
			return;
		}
		if (!scraped) {
			log.info("Scrape absences desactive, import du CSV existant");
		}

		try {
			jobOperator.start(
					absenceImportJob,
					new JobParametersBuilder()
							.addLong("run.id", System.currentTimeMillis())
							.toJobParameters());
			log.info("Import absences lance");
		} catch (Exception ex) {
			log.error("Import absences impossible: {}", ex.getMessage());
		}
	}
}
