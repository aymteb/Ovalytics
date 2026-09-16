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

import com.ovalytics.backend.domain.MatchAppearance;
import com.ovalytics.backend.repository.MatchAppearanceRepository;

@Configuration
@EnableConfigurationProperties(PlayerAppearanceImportProperties.class)
public class PlayerAppearanceImportJobConfig {

	@Bean
	public FlatFileItemReader<PlayerAppearanceCsvRow> playerAppearanceCsvReader(
			PlayerAppearanceImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<PlayerAppearanceCsvRow>()
				.name("playerAppearanceCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/player-appearances-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names(
						"competitionCode",
						"teamShortName",
						"playerName",
						"matchday",
						"homeShortName",
						"awayShortName",
						"jerseyNumber",
						"minutesPlayed",
						"tries",
						"yellowCards",
						"redCards")
				.fieldSetMapper(fields -> new PlayerAppearanceCsvRow(
						fields.readString("competitionCode"),
						fields.readString("teamShortName"),
						fields.readString("playerName"),
						fields.readInt("matchday"),
						fields.readString("homeShortName"),
						fields.readString("awayShortName"),
						fields.readString("jerseyNumber"),
						fields.readString("minutesPlayed"),
						fields.readString("tries"),
						fields.readString("yellowCards"),
						fields.readString("redCards")))
				.build();
	}

	@Bean
	public ItemWriter<MatchAppearance> playerAppearanceWriter(MatchAppearanceRepository matchAppearanceRepository) {
		return chunk -> {
			for (MatchAppearance appearance : chunk.getItems()) {
				if (appearance == null) {
					continue;
				}
				Long playerId = appearance.getPlayer().getId();
				Long matchId = appearance.getMatch().getId();
				if (matchAppearanceRepository.findByPlayerIdAndMatchId(playerId, matchId).isEmpty()) {
					matchAppearanceRepository.save(appearance);
				}
			}
		};
	}

	@Bean
	public Step playerAppearanceImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<PlayerAppearanceCsvRow> playerAppearanceCsvReader,
			PlayerAppearanceImportProcessor playerAppearanceImportProcessor,
			ItemWriter<MatchAppearance> playerAppearanceWriter) {
		return new StepBuilder("playerAppearanceImportStep", jobRepository)
				.<PlayerAppearanceCsvRow, MatchAppearance>chunk(25, transactionManager)
				.reader(playerAppearanceCsvReader)
				.processor(playerAppearanceImportProcessor)
				.writer(playerAppearanceWriter)
				.build();
	}

	@Bean
	public Job playerAppearanceImportJob(
			JobRepository jobRepository,
			Step playerAppearanceImportStep) {
		return new JobBuilder("playerAppearanceImportJob", jobRepository)
				.start(playerAppearanceImportStep)
				.build();
	}
}
