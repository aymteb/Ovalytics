package com.ovalytics.backend.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class Top14DataLoader implements ApplicationRunner {

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public Top14DataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (competitionRepository.findByCode("TOP14").isPresent()) {
			return;
		}

		Competition top14 = competitionRepository.save(
				new Competition("Top 14", "TOP14", "2025-2026", 5));

		Map<String, Team> teams = new HashMap<>();
		List.of(
				team("Stade Toulousain", "TOU", "Toulouse", top14),
				team("Racing 92", "RAC", "Nanterre", top14),
				team("Stade Français", "SFP", "Paris", top14),
				team("RC Toulon", "TOL", "Toulon", top14),
				team("Stade Rochelais", "LAR", "La Rochelle", top14),
				team("Union Bordeaux Bègles", "UBB", "Bordeaux", top14),
				team("ASM Clermont", "ASM", "Clermont-Ferrand", top14),
				team("Lyon OU", "LOU", "Lyon", top14),
				team("Montpellier HR", "MHR", "Montpellier", top14),
				team("Castres Olympique", "CAS", "Castres", top14),
				team("Section Paloise", "PAU", "Pau", top14),
				team("Aviron Bayonnais", "BAY", "Bayonne", top14),
				team("USA Perpignan", "USAP", "Perpignan", top14),
				team("RC Vannes", "VAN", "Vannes", top14)).forEach(t -> teams.put(t.getShortName(), teamRepository.save(t)));

		rugbyMatchRepository.saveAll(List.of(
				finished(top14, teams, "TOU", "VAN", 1, LocalDateTime.of(2025, 9, 6, 21, 5), 42, 14),
				finished(top14, teams, "UBB", "ASM", 1, LocalDateTime.of(2025, 9, 6, 16, 0), 28, 21),
				finished(top14, teams, "LAR", "CAS", 1, LocalDateTime.of(2025, 9, 6, 18, 30), 24, 20),
				finished(top14, teams, "TOL", "BAY", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 33, 19),
				finished(top14, teams, "SFP", "PAU", 1, LocalDateTime.of(2025, 9, 7, 17, 0), 27, 27),
				finished(top14, teams, "RAC", "MHR", 1, LocalDateTime.of(2025, 9, 7, 15, 0), 31, 17),
				finished(top14, teams, "LOU", "USAP", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 22, 16),
				scheduled(top14, teams, "VAN", "UBB", 2, LocalDateTime.of(2025, 9, 13, 16, 0)),
				scheduled(top14, teams, "ASM", "TOU", 2, LocalDateTime.of(2025, 9, 13, 21, 5)),
				scheduled(top14, teams, "CAS", "TOL", 2, LocalDateTime.of(2025, 9, 14, 17, 0)),
				scheduled(top14, teams, "BAY", "LAR", 2, LocalDateTime.of(2025, 9, 14, 21, 5)),
				scheduled(top14, teams, "PAU", "RAC", 2, LocalDateTime.of(2025, 9, 14, 15, 0)),
				scheduled(top14, teams, "MHR", "SFP", 2, LocalDateTime.of(2025, 9, 14, 18, 30)),
				scheduled(top14, teams, "USAP", "LOU", 2, LocalDateTime.of(2025, 9, 13, 18, 30))));
	}

	private static Team team(String name, String shortName, String city, Competition competition) {
		return new Team(name, shortName, city, competition);
	}

	private static RugbyMatch finished(
			Competition competition,
			Map<String, Team> teams,
			String home,
			String away,
			int matchday,
			LocalDateTime kickoffAt,
			int homeScore,
			int awayScore) {
		return new RugbyMatch(
				competition,
				teams.get(home),
				teams.get(away),
				kickoffAt,
				matchday,
				MatchStatus.FINISHED,
				homeScore,
				awayScore);
	}

	private static RugbyMatch scheduled(
			Competition competition,
			Map<String, Team> teams,
			String home,
			String away,
			int matchday,
			LocalDateTime kickoffAt) {
		return new RugbyMatch(
				competition,
				teams.get(home),
				teams.get(away),
				kickoffAt,
				matchday,
				MatchStatus.SCHEDULED,
				null,
				null);
	}
}
