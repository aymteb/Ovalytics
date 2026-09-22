package com.ovalytics.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatasourceBootstrapTest {

	@Test
	void toJdbcAddsPrefixAndSslForPublicUrl() {
		String jdbc = DatasourceBootstrap.toJdbc(
				"postgresql://postgres:secret@host.proxy.rlwy.net:1234/railway", true);
		assertThat(jdbc).isEqualTo(
				"jdbc:postgresql://postgres:secret@host.proxy.rlwy.net:1234/railway?sslmode=require");
	}

	@Test
	void toJdbcConvertsPostgresScheme() {
		String jdbc = DatasourceBootstrap.toJdbc("postgres://u:p@h:5432/db", false);
		assertThat(jdbc).isEqualTo("jdbc:postgresql://u:p@h:5432/db");
	}

	@Test
	void parseCredsReadsUserAndPassword() {
		DatasourceBootstrap.Creds creds = DatasourceBootstrap.parseCreds(
				"postgresql://postgres:s3cret@postgres.railway.internal:5432/railway");
		assertThat(creds.user()).isEqualTo("postgres");
		assertThat(creds.password()).isEqualTo("s3cret");
	}
}
