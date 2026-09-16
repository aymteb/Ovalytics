package com.ovalytics.backend.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.CompetitionResponse;

@WebMvcTest(CompetitionsController.class)
class CompetitionsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompetitionQueryService competitionQueryService;

	@Test
	void competitionsReturnsJsonList() throws Exception {
		when(competitionQueryService.listCompetitions()).thenReturn(List.of(
				new CompetitionResponse(1L, "Top 14", "TOP14", "2025-2026"),
				new CompetitionResponse(2L, "Pro D2", "PROD2", "2025-2026")));

		mockMvc.perform(get("/api/competitions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("TOP14"))
				.andExpect(jsonPath("$[1].name").value("Pro D2"));
	}
}
