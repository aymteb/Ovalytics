package com.ovalytics.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.config.TeamRefreshProperties;
import com.ovalytics.backend.domain.PendingTeamRefresh;
import com.ovalytics.backend.repository.PendingTeamRefreshRepository;

@Service
public class TeamRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(TeamRefreshScheduler.class);

	private final TeamRefreshProperties properties;
	private final PendingTeamRefreshRepository pendingTeamRefreshRepository;
	private final TeamRefreshScraperService teamRefreshScraperService;
	private final JobOperator jobOperator;
	private final Job playerProfileImportJob;
	private final Job playerAppearanceImportJob;

	public TeamRefreshScheduler(
			TeamRefreshProperties properties,
			PendingTeamRefreshRepository pendingTeamRefreshRepository,
			TeamRefreshScraperService teamRefreshScraperService,
			JobOperator jobOperator,
			Job playerProfileImportJob,
			Job playerAppearanceImportJob) {
		this.properties = properties;
		this.pendingTeamRefreshRepository = pendingTeamRefreshRepository;
		this.teamRefreshScraperService = teamRefreshScraperService;
		this.jobOperator = jobOperator;
		this.playerProfileImportJob = playerProfileImportJob;
		this.playerAppearanceImportJob = playerAppearanceImportJob;
	}

	@Scheduled(cron = "${ovalytics.team-refresh.cron:0 0 23 * * *}")
	@Transactional
	public void processPendingTeams() {
		if (!properties.isEnabled()) {
			return;
		}

		List<PendingTeamRefresh> pending = pendingTeamRefreshRepository.findAllWithTeam();
		if (pending.isEmpty()) {
			return;
		}

		List<String> teamShorts = pending.stream()
				.map(entry -> entry.getTeam().getShortName())
				.distinct()
				.sorted()
				.toList();

		log.info("File refresh joueurs ({} club(s)): {}", teamShorts.size(), String.join(", ", teamShorts));

		boolean scraped = teamRefreshScraperService.scrapeProfiles(teamShorts);
		if (!scraped && properties.isScrapeEnabled()) {
			log.warn("Scrape ignore ou en echec, import des fiches existantes si present");
		}

		try {
			long runId = System.currentTimeMillis();
			jobOperator.start(
					playerProfileImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId)
							.toJobParameters());
			jobOperator.start(
					playerAppearanceImportJob,
					new JobParametersBuilder()
							.addLong("run.id", runId + 1)
							.toJobParameters());
		} catch (Exception ex) {
			log.error("Import fiches joueurs impossible: {}", ex.getMessage());
			return;
		}

		pendingTeamRefreshRepository.deleteAll(pending);
	}
}
