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

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.repository.AbsenceRepository;

@Configuration
@EnableConfigurationProperties(AbsenceImportProperties.class)
public class AbsenceImportJobConfig {

	@Bean
	public FlatFileItemReader<AbsenceCsvRow> absenceCsvReader(
			AbsenceImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<AbsenceCsvRow>()
				.name("absenceCsvReader")
				.resource(MatchImportResource.resolve(
						properties.getFile(),
						resourceLoader,
						"data/absences-import.csv"))
				.linesToSkip(1)
				.delimited()
				.names("competitionCode", "teamShortName", "playerName", "type", "note")
				.fieldSetMapper(fields -> new AbsenceCsvRow(
						fields.readString("competitionCode"),
						fields.readString("teamShortName"),
						fields.readString("playerName"),
						fields.readString("type"),
						fields.readString("note")))
				.build();
	}

	@Bean
	public ItemWriter<Absence> absenceWriter(AbsenceRepository absenceRepository) {
		return chunk -> absenceRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step absenceImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<AbsenceCsvRow> absenceCsvReader,
			AbsenceImportProcessor absenceImportProcessor,
			ItemWriter<Absence> absenceWriter) {
		return new StepBuilder("absenceImportStep", jobRepository)
				.<AbsenceCsvRow, Absence>chunk(25, transactionManager)
				.reader(absenceCsvReader)
				.processor(absenceImportProcessor)
				.writer(absenceWriter)
				.build();
	}

	@Bean
	public Job absenceImportJob(
			JobRepository jobRepository,
			Step absenceImportStep,
			AbsenceImportCleanupListener absenceImportCleanupListener) {
		return new JobBuilder("absenceImportJob", jobRepository)
				.listener(absenceImportCleanupListener)
				.start(absenceImportStep)
				.build();
	}
}
