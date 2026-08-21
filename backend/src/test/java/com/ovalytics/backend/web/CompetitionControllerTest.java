package com.ovalytics.backend.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.service.CompetitionQueryService;
import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.TeamResponse;

@WebMvcTest(CompetitionController.class)
class CompetitionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompetitionQueryService competitionQueryService;

	@Test
	void scheduledMatchesReturnsJsonList() throws Exception {
		MatchResponse match = sampleMatch(null, List.of(), List.of());

		when(competitionQueryService.listMatches("TOP14", MatchStatus.SCHEDULED))
				.thenReturn(List.of(match));

		mockMvc.perform(get("/api/competitions/TOP14/matches").param("status", "SCHEDULED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("SCHEDULED"))
				.andExpect(jsonPath("$[0].homeTeam.shortName").value("VAN"))
				.andExpect(jsonPath("$[0].awayTeam.shortName").value("UBB"));
	}

	@Test
	void matchDetailReturnsAnalysis() throws Exception {
		MatchResponse match = sampleMatch(
				"Vannes accueille Bordeaux-Begles. Victoire bordelaise probable.",
				List.of(),
				List.of());

		when(competitionQueryService.getMatch("TOP14", 8L)).thenReturn(match);

		mockMvc.perform(get("/api/competitions/TOP14/matches/8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(8))
				.andExpect(jsonPath("$.analysis").value(
						"Vannes accueille Bordeaux-Begles. Victoire bordelaise probable."));
	}

	@Test
	void matchDetailReturnsAbsences() throws Exception {
		MatchResponse match = sampleMatch(
				null,
				List.of(new AbsenceResponse("Maxime Lafage", "INJURED", "Genou")),
				List.of(new AbsenceResponse("Adam Coleman", "INJURED", "Epaule")));

		when(competitionQueryService.getMatch("TOP14", 8L)).thenReturn(match);

		mockMvc.perform(get("/api/competitions/TOP14/matches/8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.homeAbsences[0].playerName").value("Maxime Lafage"))
				.andExpect(jsonPath("$.homeAbsences[0].type").value("INJURED"))
				.andExpect(jsonPath("$.awayAbsences[0].playerName").value("Adam Coleman"));
	}

	private static MatchResponse sampleMatch(
			String analysis,
			List<AbsenceResponse> homeAbsences,
			List<AbsenceResponse> awayAbsences) {
		TeamResponse home = new TeamResponse(1L, "RC Vannes", "VAN", "Vannes");
		TeamResponse away = new TeamResponse(2L, "Union Bordeaux Begles", "UBB", "Bordeaux");
		return new MatchResponse(
				8L,
				2,
				LocalDateTime.of(2025, 9, 13, 16, 0),
				"SCHEDULED",
				home,
				away,
				null,
				null,
				analysis,
				homeAbsences,
				awayAbsences);
	}
}
