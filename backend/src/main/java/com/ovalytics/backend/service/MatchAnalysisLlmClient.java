package com.ovalytics.backend.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ovalytics.backend.config.AnalysisProperties;
import com.ovalytics.backend.web.dto.MatchResponse;

@Component
public class MatchAnalysisLlmClient {

	private static final Logger log = LoggerFactory.getLogger(MatchAnalysisLlmClient.class);
	private static final Pattern CONTENT_PATTERN = Pattern.compile(
			"\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

	private final AnalysisProperties properties;
	private final RestClient restClient;

	public MatchAnalysisLlmClient(AnalysisProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.create();
	}

	public Optional<String> polish(String draft, MatchResponse match) {
		if (!properties.hasApiKey()) {
			return Optional.empty();
		}

		try {
			String systemPrompt = """
					Tu es un journaliste rugby français. Améliore légèrement le style du brouillon \
					sans changer sa structure ni ses faits. Garde le ton humain, vivant, sans \
					cadratin, sans liste de statistiques brutes. \
					Tu peux conserver les stades déjà cités dans le brouillon, mais n'en invente pas. \
					N'invente aucun joueur, score ou classement. \
					Réponds uniquement avec le texte final.""";
			String userPrompt = "Match : "
					+ match.homeTeam().shortName()
					+ " - "
					+ match.awayTeam().shortName()
					+ "\n\nBrouillon :\n"
					+ draft;

			String body = """
					{
					  "model": %s,
					  "messages": [
					    {"role": "system", "content": %s},
					    {"role": "user", "content": %s}
					  ]
					}
					""".formatted(
					jsonString(properties.getModel()),
					jsonString(systemPrompt),
					jsonString(userPrompt));

			String response = restClient.post()
					.uri(trimSlash(properties.getBaseUrl()) + "/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + properties.getApiKey())
					.body(body)
					.retrieve()
					.body(String.class);

			if (response == null || response.isBlank()) {
				return Optional.empty();
			}

			String content = extractMessageContent(response);
			if (content == null || content.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(content.trim());
		} catch (Exception ex) {
			log.warn("Polish IA impossible pour match {}: {}", match.id(), ex.getMessage());
			return Optional.empty();
		}
	}

	static String extractMessageContent(String responseJson) {
		Matcher matcher = CONTENT_PATTERN.matcher(responseJson);
		String last = null;
		while (matcher.find()) {
			last = unescapeJson(matcher.group(1));
		}
		return last;
	}

	private static String jsonString(String value) {
		if (value == null) {
			return "\"\"";
		}
		String escaped = value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
		return "\"" + escaped + "\"";
	}

	private static String unescapeJson(String value) {
		return value
				.replace("\\n", "\n")
				.replace("\\r", "\r")
				.replace("\\t", "\t")
				.replace("\\\"", "\"")
				.replace("\\\\", "\\");
	}

	private static String trimSlash(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "https://api.openai.com/v1";
		}
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}
}
