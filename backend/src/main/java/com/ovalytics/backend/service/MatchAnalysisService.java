package com.ovalytics.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ovalytics.backend.config.AnalysisProperties;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;

@Service
public class MatchAnalysisService {

	private static final Logger log = LoggerFactory.getLogger(MatchAnalysisService.class);
	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	private final AnalysisProperties properties;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final CompetitionQueryService competitionQueryService;
	private final MatchAnalysisDraftWriter draftWriter;
	private final MatchAnalysisFactsBuilder factsBuilder;
	private final MatchAnalysisLlmClient llmClient;
	private final MatchSheetSyncService matchSheetSyncService;

	public MatchAnalysisService(
			AnalysisProperties properties,
			RugbyMatchRepository rugbyMatchRepository,
			CompetitionQueryService competitionQueryService,
			MatchAnalysisDraftWriter draftWriter,
			MatchAnalysisFactsBuilder factsBuilder,
			MatchAnalysisLlmClient llmClient,
			MatchSheetSyncService matchSheetSyncService) {
		this.properties = properties;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.competitionQueryService = competitionQueryService;
		this.draftWriter = draftWriter;
		this.factsBuilder = factsBuilder;
		this.llmClient = llmClient;
		this.matchSheetSyncService = matchSheetSyncService;
	}

	@Transactional
	public String generateForMatch(Long matchId) {
		RugbyMatch match = rugbyMatchRepository.findByIdWithDetails(matchId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Match not found: " + matchId));
		if (match.getStatus() != MatchStatus.SCHEDULED) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Analyse reservee aux matchs SCHEDULED");
		}
		trySyncLineups(matchId);
		return writeAndSave(match);
	}

	@Transactional
	public int generateForMatchDay() {
		LocalDate today = LocalDate.now(PARIS);
		LocalDateTime from = today.atStartOfDay();
		LocalDateTime to = today.plusDays(1).atStartOfDay();
		List<RugbyMatch> matches = rugbyMatchRepository.findByStatusAndKickoffBetween(
				MatchStatus.SCHEDULED, from, to);
		int count = 0;
		for (int i = 0; i < matches.size(); i++) {
			if (i > 0 && properties.hasApiKey()) {
				try {
					Thread.sleep(13_000L);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			RugbyMatch match = matches.get(i);
			trySyncLineups(match.getId());
			writeAndSave(match);
			count++;
		}
		log.info("Analyses jour de match: {} match(s) le {}", count, today);
		return count;
	}

	private void trySyncLineups(Long matchId) {
		try {
			boolean ok = matchSheetSyncService.syncSheetById(matchId);
			if (ok) {
				log.info("Compos synchronisees avant analyse (match {})", matchId);
			}
		} catch (RuntimeException ex) {
			log.warn("Compos indisponibles avant analyse (match {}): {}", matchId, ex.getMessage());
		}
	}

	private String writeAndSave(RugbyMatch match) {
		String code = match.getCompetition().getCode();
		MatchResponse detail = competitionQueryService.getMatch(code, match.getId());
		List<StandingRowResponse> table = competitionQueryService.standings(code);
		LocalDateTime seasonStart = match.getCompetition().getSeasonStart().atStartOfDay();
		List<RugbyMatch> finished = rugbyMatchRepository.findByCompetitionCodeAndStatus(
				code, MatchStatus.FINISHED);
		List<RugbyMatch> seasonFinishedBefore = MatchAnalysisContext.seasonFinishedBefore(
				finished, seasonStart, match.getKickoffAt());
		MatchAnalysisContext context = MatchAnalysisContext.from(
				match,
				table,
				seasonFinishedBefore);
		String facts = factsBuilder.build(detail, context, seasonFinishedBefore);
		String draft = draftWriter.write(detail, context);
		String text = llmClient.generate(facts, detail).orElse(draft);
		match.setAnalysis(text);
		return text;
	}
}
