package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.AnalysisProperties;

@Service
public class MatchAnalysisScheduler {

	private static final Logger log = LoggerFactory.getLogger(MatchAnalysisScheduler.class);

	private final AnalysisProperties properties;
	private final MatchAnalysisService matchAnalysisService;

	public MatchAnalysisScheduler(
			AnalysisProperties properties,
			MatchAnalysisService matchAnalysisService) {
		this.properties = properties;
		this.matchAnalysisService = matchAnalysisService;
	}

	@Scheduled(cron = "${ovalytics.analysis.cron:0 0 9 * * TUE}", zone = "Europe/Paris")
	public void runWeeklyWindow() {
		if (!properties.isEnabled()) {
			return;
		}
		int count = matchAnalysisService.generateForUpcomingWindow();
		log.info("Job analyse mardi: {} match(s)", count);
	}
}
