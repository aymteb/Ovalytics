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
						41)));

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

		assertThat(text).doesNotContain("1V-", "2V-", "H2H récent", "Lecture :", "—");
		assertThat(text).contains("quatrième journée");
		assertThat(text).contains("15e place");
		assertThat(text).contains("trois courtes défaites");
		assertThat(text).contains("sept points");
		assertThat(text).contains("Oyonnax");
		assertThat(text).contains("47-25");
		assertThat(text).contains("41-38");
		assertThat(text).contains("Herman Coetzee");
		assertThat(text).contains("On s'attend");
		assertThat(MatchAnalysisDraftWriter.pickFavorite(match, context)).isEqualTo("NEV");
	}
}
