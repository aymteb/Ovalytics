package com.ovalytics.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ovalytics.backend.config.NewsSyncProperties;

@Service
public class NewsScraperService {

	private static final Logger log = LoggerFactory.getLogger(NewsScraperService.class);

	private final NewsSyncProperties properties;

	public NewsScraperService(NewsSyncProperties properties) {
		this.properties = properties;
	}

	public boolean scrape() {
		if (!properties.isScrapeEnabled()) {
			return false;
		}

		Path repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
		Path script = repoRoot.resolve("scripts/scrape_news.py");
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
			log.info("Scrape actu: {}", output);
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes());
			if (!outputLog.isBlank()) {
				log.info(outputLog.trim());
			}
			boolean finished = process.waitFor(45, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				log.error("Scrape actu interrompu (timeout)");
				return false;
			}
			if (process.exitValue() != 0) {
				log.error("Scrape actu en echec (code {})", process.exitValue());
				return false;
			}
			return true;
		} catch (IOException | InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Scrape actu impossible: {}", ex.getMessage());
			return false;
		}
	}
}
