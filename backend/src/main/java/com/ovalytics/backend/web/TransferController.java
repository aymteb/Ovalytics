package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.TransferResponse;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final CompetitionQueryService competitionQueryService;

	public TransferController(CompetitionQueryService competitionQueryService) {
		this.competitionQueryService = competitionQueryService;
	}

	@GetMapping
	public List<TransferResponse> transfers() {
		return competitionQueryService.listAllTransfers();
	}
}
