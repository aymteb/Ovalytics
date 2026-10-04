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
import com.ovalytics.backend.repository.RugbyMatchRepository;

@Component
@Order(5)
public class OverdueMatchPromoter implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(OverdueMatchPromoter.class);
	private static final int LIVE_WINDOW_HOURS = 4;

	private final CompetitionRepository competitionRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public OverdueMatchPromoter(
			CompetitionRepository competitionRepository,
			RugbyMatchRepository rugbyMatchRepository) {
		this.competitionRepository = competitionRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int promoted = promoteOverdueScheduled();
		if (promoted > 0) {
			log.info("Matchs SCHEDULED passes en LIVE: {}", promoted);
		}
	}

	@Transactional
	public int promoteOverdueScheduled() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime windowStart = now.minusHours(LIVE_WINDOW_HOURS);
		int promoted = 0;
		for (Competition competition : competitionRepository.findAll()) {
			List<RugbyMatch> overdue = rugbyMatchRepository
					.findByCompetitionCodeAndStatus(competition.getCode(), MatchStatus.SCHEDULED)
					.stream()
					.filter(match -> match.getKickoffAt().isBefore(now))
					.filter(match -> !match.getKickoffAt().isBefore(windowStart))
					.toList();
			for (RugbyMatch match : overdue) {
				match.setStatus(MatchStatus.LIVE);
				rugbyMatchRepository.save(match);
				promoted++;
			}
		}
		return promoted;
	}
}
