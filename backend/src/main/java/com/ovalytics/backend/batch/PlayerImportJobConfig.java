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
@EnableConfigurationProperties(PlayerImportProperties.class)
public class PlayerImportJobConfig {

	@Bean
	public FlatFileItemReader<PlayerCsvRow> playerCsvReader(
			PlayerImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<PlayerCsvRow>()
				.name("playerCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/squads-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names(
						"competitionCode",
						"teamShortName",
						"playerName",
						"position",
						"age",
						"heightCm",
						"weightKg",
						"nationality",
						"contractType",
						"jiffStatus",
						"contractEndDate")
				.fieldSetMapper(fields -> new PlayerCsvRow(
						fields.readString("competitionCode"),
						fields.readString("teamShortName"),
						fields.readString("playerName"),
						fields.readString("position"),
						fields.readString("age"),
						fields.readString("heightCm"),
						fields.readString("weightKg"),
						fields.readString("nationality"),
						fields.readString("contractType"),
						fields.readString("jiffStatus"),
						fields.readString("contractEndDate")))
				.build();
	}

	@Bean
	public ItemWriter<Player> playerWriter(PlayerRepository playerRepository) {
		return chunk -> playerRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step playerImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<PlayerCsvRow> playerCsvReader,
			PlayerImportProcessor playerImportProcessor,
			ItemWriter<Player> playerWriter) {
		return new StepBuilder("playerImportStep", jobRepository)
				.<PlayerCsvRow, Player>chunk(25, transactionManager)
				.reader(playerCsvReader)
				.processor(playerImportProcessor)
				.writer(playerWriter)
				.build();
	}

	@Bean
	public Job playerImportJob(
			JobRepository jobRepository,
			Step playerImportStep,
			PlayerImportCleanupListener playerImportCleanupListener) {
		return new JobBuilder("playerImportJob", jobRepository)
				.listener(playerImportCleanupListener)
				.start(playerImportStep)
				.build();
	}
}
