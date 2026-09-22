package com.ovalytics.backend;

final class DatasourceBootstrap {

	private DatasourceBootstrap() {
	}

	static void apply() {
		String profile = firstNonBlank(env("SPRING_PROFILES_ACTIVE"), System.getProperty("spring.profiles.active"));
		boolean prod = profile != null && profile.contains("prod");

		String user = env("PGUSER");
		String password = env("PGPASSWORD");
		String jdbc = null;

		String host = env("PGHOST");
		String port = firstNonBlank(env("PGPORT"), "5432");
		String database = env("PGDATABASE");
		if (usable(host) && usable(database)) {
			jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + database;
		}

		if (jdbc == null) {
			Parsed parsed = parse(firstNonBlank(env("DATABASE_URL"), env("DATABASE_PUBLIC_URL")));
			if (parsed != null) {
				jdbc = parsed.jdbcUrl();
				if (!notBlank(user)) {
					user = parsed.user();
				}
				if (password == null) {
					password = parsed.password();
				}
			}
		}

		if (jdbc == null) {
			if (prod) {
				throw new IllegalStateException(
						"Profil prod: DATABASE_URL (ou PGHOST+PGDATABASE) manquant ou invalide. "
								+ "Vérifie le lien Postgres sur le service backend Railway.");
			}
			return;
		}

		System.setProperty("spring.datasource.url", jdbc);
		System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
		if (notBlank(user)) {
			System.setProperty("spring.datasource.username", user);
		}
		if (password != null) {
			System.setProperty("spring.datasource.password", password);
		}

		System.out.println("Ovalytics datasource prêt → " + sanitize(jdbc) + " user=" + (user == null ? "?" : user));
	}

	static Parsed parse(String raw) {
		if (!usable(raw)) {
			return null;
		}
		String rest = raw.trim();
		if (rest.startsWith("jdbc:")) {
			rest = rest.substring("jdbc:".length());
		}
		if (rest.startsWith("postgres://")) {
			rest = rest.substring("postgres://".length());
		} else if (rest.startsWith("postgresql://")) {
			rest = rest.substring("postgresql://".length());
		} else {
			return null;
		}

		String user = null;
		String password = null;
		int at = rest.lastIndexOf('@');
		if (at >= 0) {
			String userInfo = rest.substring(0, at);
			rest = rest.substring(at + 1);
			int colon = userInfo.indexOf(':');
			if (colon >= 0) {
				user = userInfo.substring(0, colon);
				password = userInfo.substring(colon + 1);
			} else {
				user = userInfo;
				password = "";
			}
		}

		return new Parsed("jdbc:postgresql://" + rest, user, password);
	}

	static String sanitize(String jdbcUrl) {
		int scheme = jdbcUrl.indexOf("://");
		if (scheme < 0) {
			return jdbcUrl;
		}
		return jdbcUrl.substring(0, scheme + 3) + jdbcUrl.substring(scheme + 3).replaceAll("^[^/]*@", "");
	}

	private static boolean usable(String value) {
		return notBlank(value) && !value.contains("${");
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

	record Parsed(String jdbcUrl, String user, String password) {
	}
}
