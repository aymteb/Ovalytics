package com.ovalytics.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;

@SpringBootTest
@ActiveProfiles("test")
class MatchImportJobTest {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job matchImportJob;

	@Autowired
	private RugbyMatchRepository rugbyMatchRepository;

	@Test
	void importsNewMatchdaysAndUpdatesExistingScores() throws Exception {
		assertThat(rugbyMatchRepository.existsByCompetitionAndTeamsAndMatchday(
				"TOP14", "TOU", "UBB", 3)).isFalse();

		JobExecution execution = jobOperator.start(
				matchImportJob,
				new JobParametersBuilder()
						.addLong("run.id", System.currentTimeMillis())
						.toJobParameters());

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

		assertThat(rugbyMatchRepository.existsByCompetitionAndTeamsAndMatchday(
				"TOP14", "TOU", "UBB", 3)).isTrue();
		assertThat(rugbyMatchRepository.existsByCompetitionAndTeamsAndMatchday(
				"TOP14", "UBB", "LAR", 4)).isTrue();

		RugbyMatch vanUbb = rugbyMatchRepository
				.findByCompetitionAndTeamsAndMatchday("TOP14", "VAN", "UBB", 2)
				.orElseThrow();
		assertThat(vanUbb.getStatus()).isEqualTo(MatchStatus.FINISHED);
		assertThat(vanUbb.getHomeScore()).isEqualTo(17);
		assertThat(vanUbb.getAwayScore()).isEqualTo(34);
		assertThat(vanUbb.getHomeTries()).isEqualTo(2);
		assertThat(vanUbb.getAwayTries()).isEqualTo(5);

		assertThat(rugbyMatchRepository.findByCompetitionCodeAndStatus(
				"TOP14", MatchStatus.SCHEDULED)).hasSizeGreaterThanOrEqualTo(10);
	}
}
