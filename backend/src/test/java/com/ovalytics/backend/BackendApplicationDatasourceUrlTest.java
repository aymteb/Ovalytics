package com.ovalytics.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BackendApplicationDatasourceUrlTest {

	@AfterEach
	void clearProperty() {
		System.clearProperty("spring.datasource.url");
	}

	@Test
	void normalizeAddsJdbcPrefixForPostgresqlUrl() {
		System.setProperty("spring.datasource.url", "postgresql://user:pass@host:5432/db");
		BackendApplication.normalizeDatasourceUrl();
		assertThat(System.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://user:pass@host:5432/db");
	}

	@Test
	void normalizeConvertsPostgresScheme() {
		System.setProperty("spring.datasource.url", "postgres://user:pass@host:5432/db");
		BackendApplication.normalizeDatasourceUrl();
		assertThat(System.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://user:pass@host:5432/db");
	}
}
