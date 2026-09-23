package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import com.ovalytics.backend.domain.NewsItem;
import com.ovalytics.backend.repository.NewsItemRepository;

@Configuration
@EnableConfigurationProperties(NewsImportProperties.class)
public class NewsImportJobConfig {

	@Bean
	public FlatFileItemReader<NewsCsvRow> newsCsvReader(
			NewsImportProperties properties,
			ResourceLoader resourceLoader) {
		return new FlatFileItemReaderBuilder<NewsCsvRow>()
				.name("newsCsvReader")
				.resource(MatchImportResource.resolve(properties.getFile(), resourceLoader, "data/news-import.csv"))
				.linesToSkip(1)
				.delimited()
				.strict(false)
				.names("title", "summary", "sourceUrl", "publishedAt", "source", "competitionCode", "imageUrl")
				.fieldSetMapper(NewsImportJobConfig::toRow)
				.build();
	}

	@Bean
	public ItemWriter<NewsItem> newsWriter(NewsItemRepository newsItemRepository) {
		return chunk -> newsItemRepository.saveAll(chunk.getItems());
	}

	@Bean
	public Step newsImportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<NewsCsvRow> newsCsvReader,
			NewsImportProcessor newsImportProcessor,
			ItemWriter<NewsItem> newsWriter) {
		return new StepBuilder("newsImportStep", jobRepository)
				.<NewsCsvRow, NewsItem>chunk(10, transactionManager)
				.reader(newsCsvReader)
				.processor(newsImportProcessor)
				.writer(newsWriter)
				.build();
	}

	@Bean
	public Job newsImportJob(JobRepository jobRepository, Step newsImportStep) {
		return new JobBuilder("newsImportJob", jobRepository)
				.start(newsImportStep)
				.build();
	}

	private static NewsCsvRow toRow(FieldSet fields) {
		return new NewsCsvRow(
				fields.readString("title"),
				fields.readString("summary"),
				fields.readString("sourceUrl"),
				fields.readString("publishedAt"),
				fields.readString("source"),
				fields.readString("competitionCode"),
				optional(fields, "imageUrl"));
	}

	private static String optional(FieldSet fields, String name) {
		String[] names = fields.getNames();
		if (names == null) {
			return "";
		}
		for (String fieldName : names) {
			if (name.equals(fieldName)) {
				return fields.readString(name);
			}
		}
		return "";
	}
}
