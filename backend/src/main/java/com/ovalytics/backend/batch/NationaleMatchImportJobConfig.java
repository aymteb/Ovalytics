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
@EnableConfigurationProperties(NationaleMatchImportProperties.class)
public class NationaleMatchImportJobConfig {

	@Bean
	public FlatFileItemReader<MatchCsvRow> nationaleMatchCsvReader(
			NationaleMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<MatchCsvRow>()
				.name("nationaleMatchCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/import/nationale-matches.csv"))
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
	public ItemWriter<RugbyMatch> nationaleMatchWriter(RugbyMatchRepository rugbyMatchRepository) {
		return chunk -> rugbyMatchRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step nationaleMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> nationaleMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> nationaleMatchWriter) {
		return new StepBuilder("nationaleMatchImportStep", jobRepository)
				.<MatchCsvRow, RugbyMatch>chunk(20, transactionManager)
				.reader(nationaleMatchCsvReader)
				.processor(matchImportProcessor)
				.writer(nationaleMatchWriter)
				.build();
	}

	@Bean
	public Job nationaleMatchImportJob(JobRepository jobRepository, Step nationaleMatchImportStep) {
		return new JobBuilder("nationaleMatchImportJob", jobRepository)
				.start(nationaleMatchImportStep)
				.build();
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> nationaleMatchH2hCsvReader(
			NationaleMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<MatchCsvRow>()
				.name("nationaleMatchH2hCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getH2hFile(),
						resourceLoader,
						"data/import/nationale-h2h.csv"))
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
	public Step nationaleMatchH2hImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> nationaleMatchH2hCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> nationaleMatchWriter) {
		return new StepBuilder("nationaleMatchH2hImportStep", jobRepository)
				.<MatchCsvRow, RugbyMatch>chunk(20, transactionManager)
				.reader(nationaleMatchH2hCsvReader)
				.processor(matchImportProcessor)
				.writer(nationaleMatchWriter)
				.build();
	}

	@Bean
	public Job nationaleMatchH2hImportJob(JobRepository jobRepository, Step nationaleMatchH2hImportStep) {
		return new JobBuilder("nationaleMatchH2hImportJob", jobRepository)
				.start(nationaleMatchH2hImportStep)
				.build();
	}
}
