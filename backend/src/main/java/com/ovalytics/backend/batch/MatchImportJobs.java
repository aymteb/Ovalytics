package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;

final class MatchImportJobs {

	private MatchImportJobs() {
	}

	static FlatFileItemReader<MatchCsvRow> csvReader(
			String readerName,
			Resource resource) {
		return new FlatFileItemReaderBuilder<MatchCsvRow>()
				.name(readerName)
				.resource(resource)
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

	static ItemWriter<RugbyMatch> matchWriter(RugbyMatchRepository rugbyMatchRepository) {
		return chunk -> rugbyMatchRepository.saveAll(chunk.getItems());
	}

	static Step matchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			String stepName,
			FlatFileItemReader<MatchCsvRow> reader,
			MatchImportProcessor processor,
			ItemWriter<RugbyMatch> writer,
			int chunkSize) {
		return new StepBuilder(stepName, jobRepository)
				.<MatchCsvRow, RugbyMatch>chunk(chunkSize, transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}

	static Job matchImportJob(JobRepository jobRepository, String jobName, Step step) {
		return new JobBuilder(jobName, jobRepository)
				.start(step)
				.build();
	}

	static Resource resolveFile(
			String configuredFile,
			ResourceLoader resourceLoader,
			String defaultRelativeClasspath) {
		return MatchImportResource.resolve(configuredFile, resourceLoader, defaultRelativeClasspath);
	}
}
