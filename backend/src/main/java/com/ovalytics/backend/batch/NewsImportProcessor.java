package com.ovalytics.backend.batch;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.NewsItem;
import com.ovalytics.backend.repository.NewsItemRepository;

@Component
public class NewsImportProcessor implements ItemProcessor<NewsCsvRow, NewsItem> {

	private static final Pattern WORD = Pattern.compile("[a-zàâäéèêëïîôùûüç0-9]{4,}");
	private static final Set<String> STOP_WORDS = Set.of(
			"dans", "avec", "pour", "plus", "mais", "comme", "cette", "sont", "etre", "être",
			"apres", "après", "avant", "encore", "aussi", "leur", "leurs", "tous", "tout",
			"toute", "toutes", "face", "contre", "selon", "sous", "chez", "vers", "dont",
			"sans", "tres", "très", "deux", "trois", "quatre", "cinq", "match", "matchs",
			"saison", "equipe", "équipe", "joueurs", "joueur", "rugby", "actu", "actualite",
			"actualité", "article");

	private final NewsItemRepository newsItemRepository;

	public NewsImportProcessor(NewsItemRepository newsItemRepository) {
		this.newsItemRepository = newsItemRepository;
	}

	@Override
	public NewsItem process(NewsCsvRow row) {
		LocalDateTime publishedAt = LocalDateTime.parse(row.publishedAt());
		String competitionCode = blankToNull(row.competitionCode());
		String imageUrl = blankToNull(row.imageUrl());
		String summary = blankToNull(row.summary());
		String body = decodeBody(row.body());

		return newsItemRepository.findBySourceUrl(row.sourceUrl())
				.map(existing -> {
					existing.setTitle(row.title());
					existing.setSummary(summary);
					existing.setBody(body);
					existing.setImageUrl(imageUrl);
					existing.setPublishedAt(publishedAt);
					existing.setCompetitionCode(competitionCode);
					return existing;
				})
				.orElseGet(() -> {
					if (isDuplicateTitle(row.title(), publishedAt)) {
						return null;
					}
					return new NewsItem(
							row.title(),
							summary,
							body,
							imageUrl,
							row.sourceUrl(),
							publishedAt,
							row.source(),
							competitionCode);
				});
	}

	private boolean isDuplicateTitle(String title, LocalDateTime publishedAt) {
		Set<String> words = strongWords(title);
		if (words.size() < 3) {
			return false;
		}
		LocalDateTime since = publishedAt.minusHours(24);
		List<NewsItem> recent = newsItemRepository.findByPublishedAtGreaterThanEqual(since);
		for (NewsItem item : recent) {
			Set<String> other = strongWords(item.getTitle());
			int overlap = 0;
			for (String word : words) {
				if (other.contains(word)) {
					overlap++;
				}
			}
			if (overlap >= 3) {
				return true;
			}
		}
		return false;
	}

	private static Set<String> strongWords(String title) {
		Set<String> words = new HashSet<>();
		if (title == null) {
			return words;
		}
		Matcher matcher = WORD.matcher(title.toLowerCase(Locale.ROOT));
		while (matcher.find()) {
			String word = matcher.group();
			if (!STOP_WORDS.contains(word)) {
				words.add(word);
			}
		}
		return words;
	}

	private static String decodeBody(String value) {
		String body = blankToNull(value);
		if (body == null) {
			return null;
		}
		return body.replace("\\n", "\n");
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
