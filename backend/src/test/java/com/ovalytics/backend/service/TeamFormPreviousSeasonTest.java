package com.ovalytics.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;

@SpringBootTest
@ActiveProfiles("test")
class TeamFormPreviousSeasonTest {

	@Autowired
	private CompetitionQueryService competitionQueryService;

	@Autowired
	private RugbyMatchRepository rugbyMatchRepository;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job matchImportJob;

	@Test
	void formPadsWithPreviousSeasonWhenCurrentSeasonIsShort() throws Exception {
		jobOperator.start(
				matchImportJob,
				new JobParametersBuilder()
						.addLong("run.id", System.currentTimeMillis())
						.toJobParameters());

		Long matchId = rugbyMatchRepository
				.findByCompetitionAndTeamsAndMatchday("TOP14", "VAN", "UBB", 2)
				.orElseThrow()
				.getId();

		MatchResponse match = competitionQueryService.getMatch("TOP14", matchId);
		TeamFormResponse homeForm = match.homeForm();

		assertThat(homeForm).isNotNull();
		assertThat(homeForm.played()).isEqualTo(5);
		assertThat(homeForm.fromPreviousSeason()).isEqualTo(5);
		assertThat(homeForm.results()).hasSize(5);
	}

	@Test
	void standingsIgnorePreviousSeasonMatches() {
		int maxPlayed = competitionQueryService.standings("TOP14").stream()
				.mapToInt(StandingRowResponse::played)
				.max()
				.orElse(0);

		assertThat(maxPlayed).isLessThanOrEqualTo(2);
	}
}
