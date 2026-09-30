package com.ovalytics.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.H2hSyncProperties;

@Service
public class H2hScraperService {

	private static final Logger log = LoggerFactory.getLogger(H2hScraperService.class);

	private final H2hSyncProperties properties;

	public H2hScraperService(H2hSyncProperties properties) {
		this.properties = properties;
	}

	public boolean scrape(List<String> eventIds) {
		if (!properties.isScrapeEnabled()) {
			return false;
		}
		if (eventIds == null || eventIds.isEmpty()) {
			log.warn("Aucun flashscoreEventId a scraper pour le H2H");
			return false;
		}

		Path repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
		Path script = repoRoot.resolve("scripts/scrape_flashscore_h2h.py");
		Path eventIdsFile = resolveOutput(repoRoot, properties.getEventIdsFile());

		try {
			Files.createDirectories(eventIdsFile.getParent());
			Files.write(eventIdsFile, eventIds, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			log.error("Impossible d'ecrire {}: {}", eventIdsFile, ex.getMessage());
			return false;
		}

		ProcessBuilder processBuilder = new ProcessBuilder(
				properties.getPythonCommand(),
				script.toString(),
				"--event-ids-file",
				eventIdsFile.toString(),
				"--output-top14",
				resolveOutput(repoRoot, properties.getTop14Output()).toString(),
				"--output-prod2",
				resolveOutput(repoRoot, properties.getProd2Output()).toString(),
				"--output-nationale",
				resolveOutput(repoRoot, properties.getNationaleOutput()).toString(),
				"--output-ercc",
				resolveOutput(repoRoot, properties.getErccOutput()).toString(),
				"--output-erch",
				resolveOutput(repoRoot, properties.getErchOutput()).toString(),
				"--output-urc",
				resolveOutput(repoRoot, properties.getUrcOutput()).toString(),
				"--output-prem",
				resolveOutput(repoRoot, properties.getPremOutput()).toString());
		processBuilder.directory(repoRoot.toFile());
		processBuilder.redirectErrorStream(true);

		try {
			log.info("Scrape H2H pour {} event(s)", eventIds.size());
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes());
			if (!outputLog.isBlank()) {
				log.info(outputLog.trim());
			}
			boolean finished = process.waitFor(45, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				log.error("Scrape H2H interrompu (timeout)");
				return false;
			}
			if (process.exitValue() != 0) {
				log.error("Scrape H2H en echec (code {})", process.exitValue());
				return false;
			}
			return true;
		} catch (IOException | InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Scrape H2H impossible: {}", ex.getMessage());
			return false;
		}
	}

	private static Path resolveOutput(Path repoRoot, String outputRelative) {
		Path output = Path.of(outputRelative);
		if (!output.isAbsolute()) {
			output = repoRoot.resolve(outputRelative).normalize();
		}
		return output;
	}
}
