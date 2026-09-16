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
@EnableConfigurationProperties(MatchImportProperties.class)
public class MatchImportJobConfig {

	@Bean
	public FlatFileItemReader<MatchCsvRow> matchCsvReader(
			MatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<MatchCsvRow>()
				.name("matchCsvReader")
				.resource(MatchImportResource.resolve(properties.getFile(), resourceLoader))
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
	public ItemWriter<RugbyMatch> matchWriter(RugbyMatchRepository rugbyMatchRepository) {
		return chunk -> rugbyMatchRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step matchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> matchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> matchWriter) {
		return new StepBuilder("matchImportStep", jobRepository)
				.<MatchCsvRow, RugbyMatch>chunk(10, transactionManager)
				.reader(matchCsvReader)
				.processor(matchImportProcessor)
				.writer(matchWriter)
				.build();
	}

	@Bean
	public Job matchImportJob(JobRepository jobRepository, Step matchImportStep) {
		return new JobBuilder("matchImportJob", jobRepository)
				.start(matchImportStep)
				.build();
	}
}
