package com.ovalytics.backend.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.MatchAppearanceRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;

@Component
@Order(5)
public class OverdueScheduledCleanup implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(OverdueScheduledCleanup.class);

	private final CompetitionRepository competitionRepository;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final MatchAppearanceRepository matchAppearanceRepository;

	public OverdueScheduledCleanup(
			CompetitionRepository competitionRepository,
			RugbyMatchRepository rugbyMatchRepository,
			MatchAppearanceRepository matchAppearanceRepository) {
		this.competitionRepository = competitionRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.matchAppearanceRepository = matchAppearanceRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		LocalDateTime now = LocalDateTime.now();
		int removed = 0;
		for (Competition competition : competitionRepository.findAll()) {
			List<RugbyMatch> overdue = rugbyMatchRepository
					.findByCompetitionCodeAndStatus(competition.getCode(), MatchStatus.SCHEDULED)
					.stream()
					.filter(match -> match.getKickoffAt().isBefore(now))
					.toList();
			for (RugbyMatch match : overdue) {
				matchAppearanceRepository.deleteByMatchId(match.getId());
				rugbyMatchRepository.delete(match);
				removed++;
			}
		}
		if (removed > 0) {
			log.info("Matchs SCHEDULED perimes supprimes: {}", removed);
		}
	}
}
