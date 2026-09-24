package com.ovalytics.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.CalendarSyncProperties;

@Service
public class CalendarScraperService {

	private static final Logger log = LoggerFactory.getLogger(CalendarScraperService.class);

	private final CalendarSyncProperties properties;

	public CalendarScraperService(CalendarSyncProperties properties) {
		this.properties = properties;
	}

	public boolean scrapeAll() {
		if (!properties.isScrapeEnabled()) {
			return false;
		}
		return scrapeCompetition("TOP14", properties.getTop14Output())
				&& scrapeCompetition("PROD2", properties.getProd2Output());
	}

	private boolean scrapeCompetition(String competition, String outputRelative) {
		Path repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
		Path script = repoRoot.resolve("scripts/scrape_top14.py");
		Path output = Path.of(outputRelative);
		if (!output.isAbsolute()) {
			output = repoRoot.resolve(outputRelative).normalize();
		}

		ProcessBuilder processBuilder = new ProcessBuilder(
				properties.getPythonCommand(),
				script.toString(),
				"--competition",
				competition,
				"--no-demo-map",
				"--output",
				output.toString());
		processBuilder.directory(repoRoot.toFile());
		processBuilder.redirectErrorStream(true);

		try {
			log.info("Scrape calendrier {}: {}", competition, output);
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes());
			if (!outputLog.isBlank()) {
				log.info(outputLog.trim());
			}
			boolean finished = process.waitFor(45, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				log.error("Scrape calendrier {} interrompu (timeout)", competition);
				return false;
			}
			if (process.exitValue() != 0) {
				log.error("Scrape calendrier {} en echec (code {})", competition, process.exitValue());
				return false;
			}
			return true;
		} catch (IOException | InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Scrape calendrier {} impossible: {}", competition, ex.getMessage());
			return false;
		}
	}
}
