package com.ovalytics.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.PlayerDetailResponse;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

	private final CompetitionQueryService competitionQueryService;

	public PlayerController(CompetitionQueryService competitionQueryService) {
		this.competitionQueryService = competitionQueryService;
	}

	@GetMapping("/{id}")
	public PlayerDetailResponse player(@PathVariable Long id) {
		return competitionQueryService.getPlayer(id);
	}
}
