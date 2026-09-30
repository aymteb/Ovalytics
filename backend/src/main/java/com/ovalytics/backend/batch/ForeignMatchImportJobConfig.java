package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.RugbyMatchRepository;

@Configuration
@EnableConfigurationProperties(ForeignMatchImportProperties.class)
public class ForeignMatchImportJobConfig {

	@Bean
	public ItemWriter<RugbyMatch> foreignMatchWriter(RugbyMatchRepository rugbyMatchRepository) {
		return MatchImportJobs.matchWriter(rugbyMatchRepository);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> erccMatchCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"erccMatchCsvReader",
				MatchImportJobs.resolveFile(
						properties.getErccFile(),
						resourceLoader,
						"data/import/champions-cup-matches.csv"));
	}

	@Bean
	public Step erccMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> erccMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"erccMatchImportStep",
				erccMatchCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job erccMatchImportJob(JobRepository jobRepository, Step erccMatchImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "erccMatchImportJob", erccMatchImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> erchMatchCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"erchMatchCsvReader",
				MatchImportJobs.resolveFile(
						properties.getErchFile(),
						resourceLoader,
						"data/import/challenge-cup-matches.csv"));
	}

	@Bean
	public Step erchMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> erchMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"erchMatchImportStep",
				erchMatchCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job erchMatchImportJob(JobRepository jobRepository, Step erchMatchImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "erchMatchImportJob", erchMatchImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> urcMatchCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"urcMatchCsvReader",
				MatchImportJobs.resolveFile(
						properties.getUrcFile(),
						resourceLoader,
						"data/import/urc-matches.csv"));
	}

	@Bean
	public Step urcMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> urcMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"urcMatchImportStep",
				urcMatchCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job urcMatchImportJob(JobRepository jobRepository, Step urcMatchImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "urcMatchImportJob", urcMatchImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> premMatchCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"premMatchCsvReader",
				MatchImportJobs.resolveFile(
						properties.getPremFile(),
						resourceLoader,
						"data/import/premiership-matches.csv"));
	}

	@Bean
	public Step premMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> premMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"premMatchImportStep",
				premMatchCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job premMatchImportJob(JobRepository jobRepository, Step premMatchImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "premMatchImportJob", premMatchImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> intMatchCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"intMatchCsvReader",
				MatchImportJobs.resolveFile(
						properties.getIntFile(),
						resourceLoader,
						"data/import/international-matches.csv"));
	}

	@Bean
	public Step intMatchImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> intMatchCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"intMatchImportStep",
				intMatchCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job intMatchImportJob(JobRepository jobRepository, Step intMatchImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "intMatchImportJob", intMatchImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> erccMatchH2hCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"erccMatchH2hCsvReader",
				MatchImportJobs.resolveFile(
						properties.getErccH2hFile(),
						resourceLoader,
						"data/import/champions-cup-h2h.csv"));
	}

	@Bean
	public Step erccMatchH2hImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> erccMatchH2hCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"erccMatchH2hImportStep",
				erccMatchH2hCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job erccMatchH2hImportJob(JobRepository jobRepository, Step erccMatchH2hImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "erccMatchH2hImportJob", erccMatchH2hImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> erchMatchH2hCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"erchMatchH2hCsvReader",
				MatchImportJobs.resolveFile(
						properties.getErchH2hFile(),
						resourceLoader,
						"data/import/challenge-cup-h2h.csv"));
	}

	@Bean
	public Step erchMatchH2hImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> erchMatchH2hCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"erchMatchH2hImportStep",
				erchMatchH2hCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job erchMatchH2hImportJob(JobRepository jobRepository, Step erchMatchH2hImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "erchMatchH2hImportJob", erchMatchH2hImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> urcMatchH2hCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"urcMatchH2hCsvReader",
				MatchImportJobs.resolveFile(
						properties.getUrcH2hFile(),
						resourceLoader,
						"data/import/urc-h2h.csv"));
	}

	@Bean
	public Step urcMatchH2hImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> urcMatchH2hCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"urcMatchH2hImportStep",
				urcMatchH2hCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job urcMatchH2hImportJob(JobRepository jobRepository, Step urcMatchH2hImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "urcMatchH2hImportJob", urcMatchH2hImportStep);
	}

	@Bean
	public FlatFileItemReader<MatchCsvRow> premMatchH2hCsvReader(
			ForeignMatchImportProperties properties,
			ResourceLoader resourceLoader) {
		return MatchImportJobs.csvReader(
				"premMatchH2hCsvReader",
				MatchImportJobs.resolveFile(
						properties.getPremH2hFile(),
						resourceLoader,
						"data/import/premiership-h2h.csv"));
	}

	@Bean
	public Step premMatchH2hImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<MatchCsvRow> premMatchH2hCsvReader,
			MatchImportProcessor matchImportProcessor,
			ItemWriter<RugbyMatch> foreignMatchWriter) {
		return MatchImportJobs.matchImportStep(
				jobRepository,
				transactionManager,
				"premMatchH2hImportStep",
				premMatchH2hCsvReader,
				matchImportProcessor,
				foreignMatchWriter,
				20);
	}

	@Bean
	public Job premMatchH2hImportJob(JobRepository jobRepository, Step premMatchH2hImportStep) {
		return MatchImportJobs.matchImportJob(jobRepository, "premMatchH2hImportJob", premMatchH2hImportStep);
	}
}
