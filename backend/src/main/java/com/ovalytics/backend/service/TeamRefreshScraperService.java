package com.ovalytics.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.TeamRefreshProperties;

@Service
public class TeamRefreshScraperService {

	private static final Logger log = LoggerFactory.getLogger(TeamRefreshScraperService.class);

	private final TeamRefreshProperties properties;

	public TeamRefreshScraperService(TeamRefreshProperties properties) {
		this.properties = properties;
	}

	public boolean scrapeProfiles(List<String> teamShortNames) {
		if (!properties.isScrapeEnabled() || teamShortNames.isEmpty()) {
			return false;
		}

		Path repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
		Path script = repoRoot.resolve("scripts/scrape_player_profiles.py");
		Path output = repoRoot.resolve(properties.getProfilesOutput());
		String teams = String.join(",", teamShortNames);

		ProcessBuilder processBuilder = new ProcessBuilder(
				properties.getPythonCommand(),
				script.toString(),
				"--teams",
				teams,
				"--output",
				output.toString());
		processBuilder.directory(repoRoot.toFile());
		processBuilder.redirectErrorStream(true);

		try {
			log.info("Scrape fiches joueurs: {}", teams);
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes());
			if (!outputLog.isBlank()) {
				log.info(outputLog.trim());
			}
			boolean finished = process.waitFor(45, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				log.error("Scrape fiches joueurs interrompu (timeout)");
				return false;
			}
			if (process.exitValue() != 0) {
				log.error("Scrape fiches joueurs en echec (code {})", process.exitValue());
				return false;
			}
			return true;
		} catch (IOException | InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Scrape fiches joueurs impossible: {}", ex.getMessage());
			return false;
		}
	}
}
