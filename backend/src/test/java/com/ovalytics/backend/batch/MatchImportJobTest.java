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
	void importsMatchday3FromCsv() throws Exception {
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
		assertThat(rugbyMatchRepository.findByCompetitionCodeAndStatus(
				"TOP14", MatchStatus.SCHEDULED)).hasSizeGreaterThanOrEqualTo(4);
	}
}
