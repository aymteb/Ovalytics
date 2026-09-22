package com.ovalytics.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatasourceBootstrapTest {

	@Test
	void parseRemovesCredsFromHost() {
		DatasourceBootstrap.Parsed parsed = DatasourceBootstrap.parse(
				"postgresql://postgres:s3cret@postgres.railway.internal:5432/railway");
		assertThat(parsed.jdbcUrl()).isEqualTo("jdbc:postgresql://postgres.railway.internal:5432/railway");
		assertThat(parsed.user()).isEqualTo("postgres");
		assertThat(parsed.password()).isEqualTo("s3cret");
	}

	@Test
	void parseRejectsUnresolvedPlaceholder() {
		assertThat(DatasourceBootstrap.parse("postgresql://${PGHOST}:5432/db")).isNull();
	}

	@Test
	void sanitizeHidesCredentials() {
		assertThat(DatasourceBootstrap.sanitize(
				"jdbc:postgresql://postgres:s3cret@host:5432/railway"))
				.isEqualTo("jdbc:postgresql://host:5432/railway");
	}
}
