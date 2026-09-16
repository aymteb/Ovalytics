package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.ClubMercatoResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamResponse;
import com.ovalytics.backend.web.dto.TransferResponse;

@RestController
@RequestMapping("/api/competitions/{code}")
public class CompetitionController {

	private final CompetitionQueryService competitionQueryService;

	public CompetitionController(CompetitionQueryService competitionQueryService) {
		this.competitionQueryService = competitionQueryService;
	}

	@GetMapping("/teams")
	public List<TeamResponse> teams(@PathVariable String code) {
		return competitionQueryService.listTeams(code);
	}

	@GetMapping("/matches")
	public List<MatchResponse> matches(
			@PathVariable String code,
			@RequestParam(required = false) MatchStatus status) {
		return competitionQueryService.listMatches(code, status);
	}

	@GetMapping("/matches/{matchId}")
	public MatchResponse match(
			@PathVariable String code,
			@PathVariable Long matchId) {
		return competitionQueryService.getMatch(code, matchId);
	}

	@GetMapping("/standings")
	public List<StandingRowResponse> standings(@PathVariable String code) {
		return competitionQueryService.standings(code);
	}

	@GetMapping("/transfers")
	public List<TransferResponse> transfers(@PathVariable String code) {
		return competitionQueryService.listTransfers(code);
	}

	@GetMapping("/teams/{shortName}/mercato")
	public ClubMercatoResponse clubMercato(
			@PathVariable String code,
			@PathVariable String shortName) {
		return competitionQueryService.clubMercato(code, shortName);
	}
}
