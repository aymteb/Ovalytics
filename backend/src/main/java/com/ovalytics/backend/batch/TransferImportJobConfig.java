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

import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.repository.TransferRepository;

@Configuration
@EnableConfigurationProperties(TransferImportProperties.class)
public class TransferImportJobConfig {

	@Bean
	public FlatFileItemReader<TransferCsvRow> transferCsvReader(
			TransferImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<TransferCsvRow>()
				.name("transferCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(), resourceLoader, "data/transfer-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names(
						"competitionCode",
						"playerName",
						"type",
						"transferDate",
						"fromClub",
						"toClub",
						"contractLength")
				.fieldSetMapper(fields -> new TransferCsvRow(
						fields.readString("competitionCode"),
						fields.readString("playerName"),
						fields.readString("type"),
						fields.readString("transferDate"),
						fields.readString("fromClub"),
						fields.readString("toClub"),
						fields.readString("contractLength")))
				.build();
	}

	@Bean
	public ItemWriter<Transfer> transferWriter(TransferRepository transferRepository) {
		return chunk -> transferRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step transferImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<TransferCsvRow> transferCsvReader,
			TransferImportProcessor transferImportProcessor,
			ItemWriter<Transfer> transferWriter) {
		return new StepBuilder("transferImportStep", jobRepository)
				.<TransferCsvRow, Transfer>chunk(10, transactionManager)
				.reader(transferCsvReader)
				.processor(transferImportProcessor)
				.writer(transferWriter)
				.build();
	}

	@Bean
	public Job transferImportJob(JobRepository jobRepository, Step transferImportStep) {
		return new JobBuilder("transferImportJob", jobRepository)
				.start(transferImportStep)
				.build();
	}
}
