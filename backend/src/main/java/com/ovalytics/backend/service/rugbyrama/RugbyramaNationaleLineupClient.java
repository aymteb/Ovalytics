package com.ovalytics.backend.service.rugbyrama;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import com.ovalytics.backend.config.LiveScoreProperties;

@Component
public class RugbyramaNationaleLineupClient {

	private static final String HUB_URL = "https://www.rugbyrama.fr/rugby-a-xv/nationale/";
	private static final Pattern ARTICLE_HREF = Pattern.compile(
			"href=\"(/\\d{4}/\\d{2}/\\d{2}/[^\"]*compositions?-probables[^\"]*)\"",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern ARTICLE_HREF_ALT = Pattern.compile(
			"href=\"(/\\d{4}/\\d{2}/\\d{2}/[^\"]*nationale[^\"]*compositions?-probables[^\"]*)\"",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PLAYER = Pattern.compile("(\\d{1,2})[.,]\\s*([^0-9]+?)(?=\\s*(?:\\d{1,2}[.,]|$))");
	private static final Map<String, String> LABEL_TO_SHORT = Map.ofEntries(
			Map.entry("massy", "MAS"),
			Map.entry("carcassonne", "CAR"),
			Map.entry("albi", "ALB"),
			Map.entry("mont-de-marsan", "MDM"),
			Map.entry("mont de marsan", "MDM"),
			Map.entry("chambery", "CHA"),
			Map.entry("chambéry", "CHA"),
			Map.entry("rouen", "ROU"),
			Map.entry("suresnes", "SUR"),
			Map.entry("bourgoin", "BOU"),
			Map.entry("bourgoin-jallieu", "BOU"),
			Map.entry("orleans", "ORL"),
			Map.entry("orléans", "ORL"),
			Map.entry("perigueux", "PER"),
			Map.entry("périgueux", "PER"),
			Map.entry("rennes", "REN"),
			Map.entry("vienne", "VIE"),
			Map.entry("bourg-en-bresse", "USB"),
			Map.entry("marcq-en-baroeul", "MAR"),
			Map.entry("marcq-en-barœul", "MAR"),
			Map.entry("marcq", "MAR"));

	private final RestClient restClient;

	public RugbyramaNationaleLineupClient(LiveScoreProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20))
				.build();
		this.restClient = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory(httpClient))
				.defaultHeader("User-Agent", properties.getUserAgent())
				.defaultHeader("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
				.defaultHeader("Accept-Language", "fr-FR,fr;q=0.9")
				.build();
	}

	public record LineupPlayer(
			String teamSide,
			int jerseyNumber,
			Integer position,
			String playerName,
			boolean starter,
			boolean captain) {
	}

	public List<LineupPlayer> fetchLineups(String homeShortName, String awayShortName) {
		Optional<String> articleUrl = findLatestArticleUrl();
		if (articleUrl.isEmpty()) {
			return List.of();
		}
		String page = fetch(articleUrl.get());
		if (page.isBlank()) {
			return List.of();
		}
		Map<String, TeamBlock> blocks = parseArticle(page);
		TeamBlock home = blocks.get(normalize(homeShortName));
		TeamBlock away = blocks.get(normalize(awayShortName));
		if (home == null) {
			home = blocks.values().stream()
					.filter(block -> block.shortName().equalsIgnoreCase(homeShortName))
					.findFirst()
					.orElse(null);
		}
		if (away == null) {
			away = blocks.values().stream()
					.filter(block -> block.shortName().equalsIgnoreCase(awayShortName))
					.findFirst()
					.orElse(null);
		}
		if (home == null || away == null) {
			return List.of();
		}
		List<LineupPlayer> rows = new ArrayList<>();
		rows.addAll(toPlayers(home, "HOME"));
		rows.addAll(toPlayers(away, "AWAY"));
		return rows;
	}

	private Optional<String> findLatestArticleUrl() {
		String hub = fetch(HUB_URL);
		if (hub.isBlank()) {
			return Optional.empty();
		}
		List<String> paths = new ArrayList<>();
		Matcher matcher = ARTICLE_HREF.matcher(hub);
		while (matcher.find()) {
			paths.add(matcher.group(1));
		}
		matcher = ARTICLE_HREF_ALT.matcher(hub);
		while (matcher.find()) {
			paths.add(matcher.group(1));
		}
		if (paths.isEmpty()) {
			return Optional.empty();
		}
		paths.sort(String::compareTo);
		String path = paths.get(paths.size() - 1);
		return Optional.of("https://www.rugbyrama.fr" + path);
	}

	static Map<String, TeamBlock> parseArticle(String html) {
		String plain = toPlainText(html);
		Map<String, TeamBlock> blocks = new LinkedHashMap<>();
		String[] lines = plain.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			Matcher header = Pattern.compile(
					"^([A-Za-zÀ-ÿœŒ'\\- ]+?)\\s*:\\s*(.+)$").matcher(line);
			if (!header.matches()) {
				continue;
			}
			String label = header.group(1).trim();
			String shortName = resolveShort(label);
			if (shortName == null) {
				continue;
			}
			String startersRaw = header.group(2).trim();
			String benchRaw = "";
			int look = i + 1;
			while (look < lines.length && lines[look].trim().isEmpty()) {
				look++;
			}
			if (look < lines.length) {
				String next = normalize(lines[look]);
				if (next.startsWith("remplacants")) {
					benchRaw = lines[look].replaceFirst("(?i)^.*?:\\s*", "").trim();
					i = look;
				}
			}
			List<ParsedPlayer> players = new ArrayList<>();
			players.addAll(parsePlayers(startersRaw, true));
			players.addAll(parsePlayers(benchRaw, false));
			if (!players.isEmpty()) {
				blocks.put(shortName.toLowerCase(Locale.ROOT), new TeamBlock(shortName, players));
			}
		}
		return blocks;
	}

	private static List<LineupPlayer> toPlayers(TeamBlock block, String teamSide) {
		List<LineupPlayer> rows = new ArrayList<>();
		for (ParsedPlayer player : block.players()) {
			Integer position = player.starter() && player.jersey() <= 15 ? player.jersey() : null;
			rows.add(new LineupPlayer(
					teamSide,
					player.jersey(),
					position,
					player.name(),
					player.starter(),
					player.captain()));
		}
		return rows;
	}

	static List<ParsedPlayer> parsePlayers(String raw, boolean starter) {
		List<ParsedPlayer> rows = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return rows;
		}
		String cleaned = raw
				.replace('\u00a0', ' ')
				.replace('\u2009', ' ')
				.replace('\u202f', ' ');
		Matcher matcher = PLAYER.matcher(cleaned);
		while (matcher.find()) {
			int jersey = Integer.parseInt(matcher.group(1));
			String name = cleanName(matcher.group(2));
			if (name.isBlank() || jersey <= 0) {
				continue;
			}
			boolean captain = name.toLowerCase(Locale.ROOT).contains("(cap");
			name = name.replaceAll("(?i)\\(\\s*cap\\.?\\s*\\)", "").trim();
			rows.add(new ParsedPlayer(jersey, name, starter, captain));
		}
		return rows;
	}

	static String cleanName(String raw) {
		String name = raw
				.replace('\u00a0', ' ')
				.replace('\u2009', ' ')
				.replace('\u202f', ' ')
				.replace('\u200b', ' ')
				.replace('\u2060', ' ')
				.trim();
		name = name.replaceAll("[;,\\.]+$", "").trim();
		int ou = name.toLowerCase(Locale.ROOT).indexOf(" ou ");
		if (ou > 0) {
			name = name.substring(0, ou).trim();
		}
		return name.replaceAll("\\s+", " ").trim();
	}

	static String resolveShort(String label) {
		String key = normalize(label);
		if (key.isEmpty()) {
			return null;
		}
		for (Map.Entry<String, String> entry : LABEL_TO_SHORT.entrySet()) {
			String entryKey = normalize(entry.getKey());
			if (key.equals(entryKey) || key.contains(entryKey) || entryKey.contains(key)) {
				return entry.getValue();
			}
		}
		return null;
	}

	static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase(Locale.ROOT)
				.replace("é", "e").replace("è", "e").replace("ê", "e")
				.replace("à", "a").replace("â", "a")
				.replace("ô", "o").replace("î", "i")
				.replace("ù", "u").replace("ç", "c")
				.replace("œ", "oe");
		return lower.replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
	}

	private static String toPlainText(String html) {
		String text = HtmlUtils.htmlUnescape(html == null ? "" : html);
		text = text.replaceAll("(?is)<script.*?</script>", " ");
		text = text.replaceAll("(?is)<style.*?</style>", " ");
		text = text.replaceAll("(?i)<br\\s*/?>", "\n");
		text = text.replaceAll("(?i)</p>", "\n");
		text = text.replaceAll("(?i)</(?:div|h[1-6]|li|tr)>", "\n");
		text = text.replaceAll("<[^>]+>", " ");
		text = text.replace('\u00a0', ' ');
		text = text.replace('\u2009', ' ');
		text = text.replace('\u202f', ' ');
		text = text.replace('\u200b', ' ');
		text = text.replace('\u2060', ' ');
		text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
		text = text.replaceAll(" *\\n *", "\n");
		return text;
	}

	private String fetch(String url) {
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.retrieve()
					.body(String.class);
			return body == null ? "" : body;
		} catch (RuntimeException ex) {
			return "";
		}
	}

	record TeamBlock(String shortName, List<ParsedPlayer> players) {
	}

	record ParsedPlayer(int jersey, String name, boolean starter, boolean captain) {
	}
}
