package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.repository.PlayerRepository;

@Configuration
@EnableConfigurationProperties(PlayerProfileImportProperties.class)
public class PlayerProfileImportJobConfig {

	@Bean
	public FlatFileItemReader<PlayerProfileCsvRow> playerProfileCsvReader(
			PlayerProfileImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<PlayerProfileCsvRow>()
				.name("playerProfileCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/player-profiles-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names(
						"competitionCode",
						"teamShortName",
						"playerName",
						"profileUrl",
						"seasonMatches",
						"seasonStarts",
						"seasonMinutes",
						"seasonTries",
						"seasonYellowCards",
						"seasonRedCards",
						"contractEndDate",
						"careerHistory")
				.fieldSetMapper(fields -> new PlayerProfileCsvRow(
						fields.readString("competitionCode"),
						fields.readString("teamShortName"),
						fields.readString("playerName"),
						fields.readString("profileUrl"),
						fields.readString("seasonMatches"),
						fields.readString("seasonStarts"),
						fields.readString("seasonMinutes"),
						fields.readString("seasonTries"),
						fields.readString("seasonYellowCards"),
						fields.readString("seasonRedCards"),
						fields.readString("contractEndDate"),
						fields.readString("careerHistory")))
				.build();
	}

	@Bean
	public ItemWriter<Player> playerProfileWriter(PlayerRepository playerRepository) {
		return chunk -> playerRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step playerProfileImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<PlayerProfileCsvRow> playerProfileCsvReader,
			PlayerProfileImportProcessor playerProfileImportProcessor,
			ItemWriter<Player> playerProfileWriter) {
		return new StepBuilder("playerProfileImportStep", jobRepository)
				.<PlayerProfileCsvRow, Player>chunk(25, transactionManager)
				.reader(playerProfileCsvReader)
				.processor(playerProfileImportProcessor)
				.writer(playerProfileWriter)
				.build();
	}

	@Bean
	public Job playerProfileImportJob(
			JobRepository jobRepository,
			Step playerProfileImportStep) {
		return new JobBuilder("playerProfileImportJob", jobRepository)
				.start(playerProfileImportStep)
				.build();
	}
}
