package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.CompetitionResponse;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionsController {

	private final CompetitionQueryService competitionQueryService;

	public CompetitionsController(CompetitionQueryService competitionQueryService) {
		this.competitionQueryService = competitionQueryService;
	}

	@GetMapping
	public List<CompetitionResponse> competitions() {
		return competitionQueryService.listCompetitions();
	}
}
