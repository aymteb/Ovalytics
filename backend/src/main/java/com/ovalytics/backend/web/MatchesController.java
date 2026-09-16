package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.MatchResponse;

@RestController
@RequestMapping("/api/matches")
public class MatchesController {

	private final CompetitionQueryService competitionQueryService;

	public MatchesController(CompetitionQueryService competitionQueryService) {
		this.competitionQueryService = competitionQueryService;
	}

	@GetMapping
	public List<MatchResponse> matches(@RequestParam(required = false) MatchStatus status) {
		return competitionQueryService.listAllMatches(status);
	}

	@GetMapping("/{matchId}")
	public MatchResponse match(@PathVariable Long matchId) {
		return competitionQueryService.getMatchById(matchId);
	}
}
