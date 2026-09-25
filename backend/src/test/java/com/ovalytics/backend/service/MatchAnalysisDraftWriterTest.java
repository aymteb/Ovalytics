package com.ovalytics.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.HeadToHeadMatchResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;
import com.ovalytics.backend.web.dto.TeamResponse;
import com.ovalytics.backend.web.dto.VenueRecordResponse;

class MatchAnalysisDraftWriterTest {

	private final MatchAnalysisDraftWriter writer = new MatchAnalysisDraftWriter();

	@Test
	void whenPhraseUsesYearWhenOlderThanTwelveMonths() {
		LocalDateTime reference = LocalDateTime.of(2026, 9, 18, 19, 30);
		assertThat(MatchAnalysisDraftWriter.whenPhrase(
				LocalDateTime.of(2025, 5, 8, 19, 0), reference))
				.isEqualTo(" en mai 2025");
		assertThat(MatchAnalysisDraftWriter.whenPhrase(
				LocalDateTime.of(2026, 4, 10, 17, 30), reference))
				.isEqualTo(" en avril dernier");
	}

	@Test
	void draftFollowsNarrativeSocleForNeversBiarritzStyle() {
		MatchResponse match = new MatchResponse(
				244L,
				"PROD2",
				"Pro D2",
				4,
				LocalDateTime.of(2026, 9, 18, 19, 30),
				"SCHEDULED",
				new TeamResponse(18L, "USON Nevers", "NEV", "Nevers"),
				new TeamResponse(21L, "Biarritz Olympique", "BIA", "Biarritz"),
				null,
				null,
				null,
				null,
				null,
				List.of(
						new AbsenceResponse("Herman Coetzee", "INJURED", null),
						new AbsenceResponse("Rati Zazadze", "INJURED", null),
						new AbsenceResponse("Wendemi Viellard", "INJURED", null)),
				List.of(),
				new TeamFormResponse(List.of("D", "D", "D", "V", "V"), 5, 2, 0, 3, 2),
				new TeamFormResponse(List.of("V", "D", "V", "V", "D"), 5, 3, 0, 2, 2),
				new VenueRecordResponse(18, 14, 2, 2),
				new VenueRecordResponse(20, 1, 1, 18),
				List.of(new HeadToHeadMatchResponse(
						593L,
						LocalDateTime.of(2026, 4, 10, 17, 30),
						"BIA",
						"NEV",
						38,
						41)),
				List.of(),
				List.of());

		MatchAnalysisContext context = new MatchAnalysisContext(
				new StandingRowResponse(15, 18L, "USON Nevers", "NEV", 3, 0, 0, 3, 67, 74, -7, 3, 3),
				new StandingRowResponse(4, 21L, "Biarritz Olympique", "BIA", 3, 2, 0, 1, 99, 72, 27, 2, 10),
				List.of(
						new MatchAnalysisContext.NarrowLoss(1),
						new MatchAnalysisContext.NarrowLoss(2),
						new MatchAnalysisContext.NarrowLoss(4)),
				new MatchAnalysisContext.ScoredOuting("US Oyonnax", "OYO", 25, 47, true),
				1,
				16);

		String text = writer.write(match, context);

		assertThat(text).doesNotContain("1V-", "H2H récent", "Lecture :", "—", "mai dernier");
		assertThat(text).contains("Pré-Fleuri");
		assertThat(text).contains("15e place");
		assertThat(text).contains("Oyonnax");
		assertThat(text).contains("47-25");
		assertThat(text).contains("41-38");
		assertThat(text).contains("avril dernier");
		assertThat(text).containsAnyOf("victoire", "succès", "emporter", "final");
	}

	@Test
	void draftMontaubanBeziersMentionsSapiacAndCorrectH2hYear() {
		MatchResponse match = new MatchResponse(
				242L,
				"PROD2",
				"Pro D2",
				4,
				LocalDateTime.of(2026, 9, 18, 19, 30),
				"SCHEDULED",
				new TeamResponse(28L, "US Montauban", "MTB", "Montauban"),
				new TeamResponse(15L, "AS Béziers", "BEZ", "Béziers"),
				null,
				null,
				null,
				null,
				null,
				List.of(new AbsenceResponse("Maël Castel", "INJURED", null)),
				List.of(),
				new TeamFormResponse(List.of("V", "V", "D"), 3, 2, 0, 1, 0),
				new TeamFormResponse(List.of("V", "D", "V"), 3, 2, 0, 1, 0),
				new VenueRecordResponse(10, 7, 1, 2),
				new VenueRecordResponse(10, 3, 1, 6),
				List.of(new HeadToHeadMatchResponse(
						533L,
						LocalDateTime.of(2025, 5, 8, 19, 0),
						"MTB",
						"BEZ",
						42,
						17)),
				List.of(),
				List.of());

		MatchAnalysisContext context = new MatchAnalysisContext(
				new StandingRowResponse(8, 28L, "US Montauban", "MTB", 3, 2, 0, 1, 70, 55, 15, 1, 9),
				new StandingRowResponse(10, 15L, "AS Béziers", "BEZ", 3, 1, 0, 2, 60, 65, -5, 1, 5),
				List.of(),
				null,
				1,
				16);

		String text = writer.write(match, context);

		assertThat(text).contains("Sapiac");
		assertThat(text).contains("Montauban reçoit Béziers");
		assertThat(text).contains("mai 2025");
		assertThat(text).doesNotContain("mai dernier");
		assertThat(text).doesNotContain("US Montauban");
		assertThat(text).doesNotContain("AS Béziers");
		assertThat(text).contains("42-17");
		assertThat(text).containsAnyOf("locaux", "emporter", "bonus", "score");
	}
}
