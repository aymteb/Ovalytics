package com.ovalytics.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.AbsenceSyncProperties;

@Service
public class AbsenceScraperService {

	private static final Logger log = LoggerFactory.getLogger(AbsenceScraperService.class);

	private final AbsenceSyncProperties properties;

	public AbsenceScraperService(AbsenceSyncProperties properties) {
		this.properties = properties;
	}

	public boolean scrape() {
		if (!properties.isScrapeEnabled()) {
			return false;
		}

		Path repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
		Path script = repoRoot.resolve("scripts/scrape_absences.py");
		Path output = Path.of(properties.getOutput());
		if (!output.isAbsolute()) {
			output = repoRoot.resolve(properties.getOutput()).normalize();
		}

		ProcessBuilder processBuilder = new ProcessBuilder(
				properties.getPythonCommand(),
				script.toString(),
				"--output",
				output.toString());
		processBuilder.directory(repoRoot.toFile());
		processBuilder.redirectErrorStream(true);

		try {
			log.info("Scrape absences: {}", output);
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes());
			if (!outputLog.isBlank()) {
				log.info(outputLog.trim());
			}
			boolean finished = process.waitFor(20, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				log.error("Scrape absences interrompu (timeout)");
				return false;
			}
			if (process.exitValue() != 0) {
				log.error("Scrape absences en echec (code {})", process.exitValue());
				return false;
			}
			return true;
		} catch (IOException | InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Scrape absences impossible: {}", ex.getMessage());
			return false;
		}
	}
}
