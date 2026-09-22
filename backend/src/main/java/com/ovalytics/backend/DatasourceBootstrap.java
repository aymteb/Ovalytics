package com.ovalytics.backend;

final class DatasourceBootstrap {

	private DatasourceBootstrap() {
	}

	static void apply() {
		String user = env("PGUSER");
		String password = env("PGPASSWORD");
		String database = env("PGDATABASE");

		String jdbc = null;

		String publicUrl = env("DATABASE_PUBLIC_URL");
		if (notBlank(publicUrl)) {
			jdbc = toJdbc(publicUrl, true);
			Creds creds = parseCreds(publicUrl);
			if (creds != null) {
				if (!notBlank(user)) {
					user = creds.user();
				}
				if (password == null) {
					password = creds.password();
				}
			}
		}

		if (jdbc == null) {
			String proxyHost = env("RAILWAY_TCP_PROXY_DOMAIN");
			String proxyPort = env("RAILWAY_TCP_PROXY_PORT");
			if (notBlank(proxyHost) && notBlank(proxyPort) && notBlank(database)) {
				jdbc = "jdbc:postgresql://" + proxyHost + ":" + proxyPort + "/" + database + "?sslmode=require";
			}
		}

		if (jdbc == null) {
			String host = env("PGHOST");
			String port = firstNonBlank(env("PGPORT"), "5432");
			if (notBlank(host) && notBlank(database)) {
				boolean publicHost = !host.contains("railway.internal");
				jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + database;
				if (publicHost) {
					jdbc = withSslRequire(jdbc);
				}
			}
		}

		if (jdbc == null) {
			String raw = firstNonBlank(env("DATABASE_URL"), env("SPRING_DATASOURCE_URL"));
			if (notBlank(raw)) {
				boolean requireSsl = !raw.contains("railway.internal");
				jdbc = toJdbc(raw, requireSsl);
				Creds creds = parseCreds(raw);
				if (creds != null) {
					if (!notBlank(user)) {
						user = creds.user();
					}
					if (password == null) {
						password = creds.password();
					}
				}
			}
		}

		if (jdbc != null) {
			System.setProperty("spring.datasource.url", jdbc);
		}
		if (notBlank(user)) {
			System.setProperty("spring.datasource.username", user);
		}
		if (password != null) {
			System.setProperty("spring.datasource.password", password);
		}
		if (jdbc != null || notBlank(user) || password != null) {
			System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
		}
	}

	static String toJdbc(String url, boolean requireSsl) {
		String value = url.trim();
		if (value.startsWith("postgres://")) {
			value = "jdbc:postgresql://" + value.substring("postgres://".length());
		} else if (value.startsWith("postgresql://")) {
			value = "jdbc:" + value;
		} else if (!value.startsWith("jdbc:")) {
			value = "jdbc:postgresql://" + value;
		}
		if (requireSsl) {
			value = withSslRequire(value);
		}
		return value;
	}

	static String withSslRequire(String jdbcUrl) {
		if (jdbcUrl.contains("sslmode=")) {
			return jdbcUrl;
		}
		return jdbcUrl.contains("?") ? jdbcUrl + "&sslmode=require" : jdbcUrl + "?sslmode=require";
	}

	static Creds parseCreds(String url) {
		String value = url.trim();
		if (value.startsWith("jdbc:")) {
			value = value.substring("jdbc:".length());
		}
		if (value.startsWith("postgres://")) {
			value = value.substring("postgres://".length());
		} else if (value.startsWith("postgresql://")) {
			value = value.substring("postgresql://".length());
		} else {
			return null;
		}
		int at = value.lastIndexOf('@');
		if (at <= 0) {
			return null;
		}
		String userInfo = value.substring(0, at);
		int colon = userInfo.indexOf(':');
		if (colon < 0) {
			return new Creds(userInfo, "");
		}
		return new Creds(userInfo.substring(0, colon), userInfo.substring(colon + 1));
	}

	private static String env(String name) {
		return System.getenv(name);
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (notBlank(value)) {
				return value;
			}
		}
		return null;
	}

	record Creds(String user, String password) {
	}
}
