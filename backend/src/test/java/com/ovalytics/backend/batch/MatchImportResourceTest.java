package com.ovalytics.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

class MatchImportResourceTest {

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Test
	void blankUsesClasspathDefault() {
		Resource resource = MatchImportResource.resolve("", resourceLoader);
		assertThat(resource).isInstanceOf(ClassPathResource.class);
		assertThat(resource.getFilename()).isEqualTo("top14-import.csv");
	}

	@Test
	void absolutePathUsesFileSystem() {
		Resource resource = MatchImportResource.resolve("/tmp/scrap-matches.csv", resourceLoader);
		assertThat(resource).isInstanceOf(FileSystemResource.class);
	}

	@Test
	void classpathPrefixIsRespected() {
		Resource resource = MatchImportResource.resolve(
				"classpath:data/top14-import.csv",
				resourceLoader);
		assertThat(resource).isInstanceOf(ClassPathResource.class);
		assertThat(resource.exists()).isTrue();
	}
}
