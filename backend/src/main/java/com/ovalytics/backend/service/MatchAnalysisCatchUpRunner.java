package com.ovalytics.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.config.AnalysisProperties;

@Component
@Order(50)
public class MatchAnalysisCatchUpRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MatchAnalysisCatchUpRunner.class);

	private final AnalysisProperties properties;
	private final MatchAnalysisService matchAnalysisService;

	public MatchAnalysisCatchUpRunner(
			AnalysisProperties properties,
			MatchAnalysisService matchAnalysisService) {
		this.properties = properties;
		this.matchAnalysisService = matchAnalysisService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled() || !properties.isCatchUpOnStartup()) {
			return;
		}
		int count = matchAnalysisService.generateForUpcomingWindow();
		log.info("Rattrapage analyses au demarrage: {} match(s)", count);
	}
}
