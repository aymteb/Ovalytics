package com.ovalytics.backend.service;

import java.time.LocalDateTime;
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

	private final AnalysisProperties properties;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final CompetitionQueryService competitionQueryService;
	private final MatchAnalysisDraftWriter draftWriter;
	private final MatchAnalysisLlmClient llmClient;

	public MatchAnalysisService(
			AnalysisProperties properties,
			RugbyMatchRepository rugbyMatchRepository,
			CompetitionQueryService competitionQueryService,
			MatchAnalysisDraftWriter draftWriter,
			MatchAnalysisLlmClient llmClient) {
		this.properties = properties;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.competitionQueryService = competitionQueryService;
		this.draftWriter = draftWriter;
		this.llmClient = llmClient;
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
		return writeAndSave(match);
	}

	@Transactional
	public int generateForUpcomingWindow() {
		LocalDateTime from = LocalDateTime.now();
		LocalDateTime to = from.plusDays(properties.getWindowDays());
		List<RugbyMatch> matches = rugbyMatchRepository.findByStatusAndKickoffBetween(
				MatchStatus.SCHEDULED, from, to);
		int count = 0;
		for (RugbyMatch match : matches) {
			writeAndSave(match);
			count++;
		}
		log.info("Analyses generees: {} match(s) entre {} et {}", count, from, to);
		return count;
	}

	private String writeAndSave(RugbyMatch match) {
		String code = match.getCompetition().getCode();
		MatchResponse detail = competitionQueryService.getMatch(code, match.getId());
		List<StandingRowResponse> table = competitionQueryService.standings(code);
		LocalDateTime seasonStart = match.getCompetition().getSeasonStart().atStartOfDay();
		List<RugbyMatch> finished = rugbyMatchRepository.findByCompetitionCodeAndStatus(
				code, MatchStatus.FINISHED);
		MatchAnalysisContext context = MatchAnalysisContext.from(
				match,
				table,
				MatchAnalysisContext.seasonFinishedBefore(
						finished, seasonStart, match.getKickoffAt()));
		String draft = draftWriter.write(detail, context);
		String text = llmClient.polish(draft, detail).orElse(draft);
		match.setAnalysis(text);
		return text;
	}
}
