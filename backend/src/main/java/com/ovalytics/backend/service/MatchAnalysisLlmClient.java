package com.ovalytics.backend.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.config.AnalysisProperties;
import com.ovalytics.backend.web.dto.MatchResponse;

@Component
public class MatchAnalysisLlmClient {

	private static final Logger log = LoggerFactory.getLogger(MatchAnalysisLlmClient.class);

	private final AnalysisProperties properties;
	private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

	public MatchAnalysisLlmClient(
			AnalysisProperties properties,
			ObjectProvider<ChatClient.Builder> chatClientBuilder) {
		this.properties = properties;
		this.chatClientBuilder = chatClientBuilder;
	}

	public Optional<String> generate(String facts, MatchResponse match) {
		if (!properties.hasApiKey()) {
			return Optional.empty();
		}
		ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
		if (builder == null) {
			log.warn("ChatClient Gemini indisponible (Spring AI non configure)");
			return Optional.empty();
		}

		String systemPrompt = """
				Tu es un journaliste rugby francais pour le site Ovalytics.
				Tu rediges la rubrique "Notre lecture" d'avant-match, le matin du match.
				Tu t'appuies UNIQUEMENT sur les faits fournis (indispos, forme, compos si presentes).
				Contraintes:
				- un seul texte suivi, 120 a 220 mots, en francais
				- ton vivant et engage: tu proposes un vainqueur probable et un eventuel bonus,
				  mais tu restes nuance si les faits sont minces (peu de matchs joues,
				  absences non datees, forme incomplete)
				- si les compos sont dans les faits, tu peux t'en servir; sinon tu le dis
				  brievement (XV pas encore figes) et tu n'inventes aucun titulaire
				- n'invente aucun joueur, score, classement, carton, stade ou date de retour
				- ne transforme pas une longue liste d'absents en verdict categorique
				  ("carnage", "sans hesiter") si le reste des faits est fragile
				- pas de listes a puces, pas de cadratin, pas de titre
				- si une info manque, ne la comble pas
				Reponds uniquement avec le texte final, sans raisonnement ni brouillon.""";
		String userPrompt = "Match : "
				+ match.homeTeam().shortName()
				+ " - "
				+ match.awayTeam().shortName()
				+ "\n\nFaits:\n"
				+ facts;

		return callWithRetry(builder, systemPrompt, userPrompt, match.id());
	}

	public record PingResult(
			boolean apiKeyConfigured,
			boolean chatClientAvailable,
			boolean ok,
			String reply,
			String error) {
	}

	public PingResult ping() {
		boolean hasKey = properties.hasApiKey();
		ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
		boolean clientOk = builder != null;
		if (!hasKey) {
			return new PingResult(false, clientOk, false, null, "Cle API absente (OVALYTICS_ANALYSIS_API_KEY)");
		}
		if (!clientOk) {
			return new PingResult(true, false, false, null, "ChatClient Spring AI indisponible");
		}
		try {
			String reply = builder.build()
					.prompt()
					.user("Reponds exactement par le mot OK, rien d'autre.")
					.call()
					.content();
			if (reply == null || reply.isBlank()) {
				return new PingResult(true, true, false, null, "Reponse vide");
			}
			return new PingResult(true, true, true, reply.trim(), null);
		} catch (Exception ex) {
			return new PingResult(true, true, false, null, rootMessage(ex));
		}
	}

	private Optional<String> callWithRetry(
			ChatClient.Builder builder,
			String systemPrompt,
			String userPrompt,
			Long matchId) {
		Exception lastError = null;
		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				String content = builder.build()
						.prompt()
						.system(systemPrompt)
						.user(userPrompt)
						.call()
						.content();
				if (content == null || content.isBlank()) {
					return Optional.empty();
				}
				return Optional.of(content.trim());
			} catch (Exception ex) {
				lastError = ex;
				boolean retryable = isRetryable(ex);
				if (!retryable || attempt == maxAttempts) {
					break;
				}
				long waitMs = 12_000L * (1L << (attempt - 1));
				log.warn(
						"Gemini temporairement indisponible pour match {} (essai {}/{}), nouvel essai dans {}s: {}",
						matchId,
						attempt,
						maxAttempts,
						waitMs / 1000,
						rootMessage(ex));
				try {
					Thread.sleep(waitMs);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		log.warn(
				"Analyse Gemini impossible pour match {}: {}",
				matchId,
				rootMessage(lastError));
		return Optional.empty();
	}

	private static boolean isRetryable(Exception ex) {
		Throwable current = ex;
		while (current != null) {
			String message = current.getMessage() == null ? "" : current.getMessage();
			if (message.contains("503")
					|| message.contains("UNAVAILABLE")
					|| message.contains("high demand")
					|| message.contains("429")
					|| message.contains("RESOURCE_EXHAUSTED")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static String rootMessage(Exception ex) {
		if (ex == null) {
			return "reponse vide";
		}
		Throwable current = ex;
		String last = ex.getMessage();
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
			if (current.getMessage() != null && !current.getMessage().isBlank()) {
				last = current.getMessage();
			}
		}
		return last == null || last.isBlank() ? ex.toString() : last;
	}
}
