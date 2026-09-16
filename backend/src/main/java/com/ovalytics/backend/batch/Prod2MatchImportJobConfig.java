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

import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;

@Configuration
@EnableConfigurationProperties(Prod2MatchImportProperties.class)
public class Prod2MatchImportJobConfig {

	@Bean
	public FlatFileItemReader<MatchCsvRow> prod2MatchCsvReader(
			Prod2MatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<MatchCsvRow>()
				.name("prod2MatchCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/prod2-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names(
						"competitionCode",
						"homeShortName",
						"awayShortName",
						"matchday",
						"kickoffAt",
						"status",
						"homeScore",
						"awayScore",
						"homeTries",
						"awayTries")
				.fieldSetMapper(fields -> new MatchCsvRow(
						fields.readString("competitionCode"),
						fields.readString("homeShortName"),
						fields.readString("awayShortName"),
						fields.readInt("matchday"),
						fields.readString("kickoffAt"),
						fields.readString("status"),
						fields.readString("homeScore"),
						fields.readString("awayScore"),
						fields.readString("homeTries"),
						fields.readString("awayTries")))
				.build();
	}

	@Bean
	public ItemWriter<RugbyMatch> prod2MatchWriter(RugbyMatchRepository rugbyMatchRepository) {
		return chunk -> rugbyMatchRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step prod2MatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> prod2MatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> prod2MatchWriter) {
		return new StepBuilder("prod2MatchImportStep", jobRepository)
				.<MatchCsvRow, RugbyMatch>chunk(10, transactionManager)
				.reader(prod2MatchCsvReader)
				.processor(matchImportProcessor)
				.writer(prod2MatchWriter)
				.build();
	}

	@Bean
	public Job prod2MatchImportJob(JobRepository jobRepository, Step prod2MatchImportStep) {
		return new JobBuilder("prod2MatchImportJob", jobRepository)
				.start(prod2MatchImportStep)
				.build();
	}
}
