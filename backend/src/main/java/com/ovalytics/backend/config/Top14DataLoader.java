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

	private static final String VAN_UBB_ANALYSIS = """
			Vannes accueille Bordeaux-Bègles avec l'ambition de poser un vrai problème à domicile. \
			L'UBB reste favori sur le papier, plus dense devant et plus tranchant derrière. \
			Scénario probable : victoire bordelaise, avec un bonus offensif possible si le rythme monte tôt. \
			Réserve classique : carton, mêlée qui s'effondre, ou Vannes qui verrouille le score.""";

	private static final String ASM_TOU_ANALYSIS = """
			Clermont-Toulouse, c'est souvent une affiche de caractère. Toulouse arrive avec plus de solutions \
			et une habitude des gros matchs ; Clermont peut basculer le match s'il impose son pack. \
			Lecture : victoire toulousaine, sans forcément le bonus. \
			Tout peut basculer sur les absences de dernière minute et la discipline.""";

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
			fillMissingAnalyses();
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
				finished(top14, teams, "TOU", "VAN", 1, LocalDateTime.of(2025, 9, 6, 21, 5), 42, 14, null),
				finished(top14, teams, "UBB", "ASM", 1, LocalDateTime.of(2025, 9, 6, 16, 0), 28, 21, null),
				finished(top14, teams, "LAR", "CAS", 1, LocalDateTime.of(2025, 9, 6, 18, 30), 24, 20, null),
				finished(top14, teams, "TOL", "BAY", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 33, 19, null),
				finished(top14, teams, "SFP", "PAU", 1, LocalDateTime.of(2025, 9, 7, 17, 0), 27, 27, null),
				finished(top14, teams, "RAC", "MHR", 1, LocalDateTime.of(2025, 9, 7, 15, 0), 31, 17, null),
				finished(top14, teams, "LOU", "USAP", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 22, 16, null),
				scheduled(top14, teams, "VAN", "UBB", 2, LocalDateTime.of(2025, 9, 13, 16, 0), VAN_UBB_ANALYSIS),
				scheduled(top14, teams, "ASM", "TOU", 2, LocalDateTime.of(2025, 9, 13, 21, 5), ASM_TOU_ANALYSIS),
				scheduled(top14, teams, "CAS", "TOL", 2, LocalDateTime.of(2025, 9, 14, 17, 0), null),
				scheduled(top14, teams, "BAY", "LAR", 2, LocalDateTime.of(2025, 9, 14, 21, 5), null),
				scheduled(top14, teams, "PAU", "RAC", 2, LocalDateTime.of(2025, 9, 14, 15, 0), null),
				scheduled(top14, teams, "MHR", "SFP", 2, LocalDateTime.of(2025, 9, 14, 18, 30), null),
				scheduled(top14, teams, "USAP", "LOU", 2, LocalDateTime.of(2025, 9, 13, 18, 30), null)));
	}

	private void fillMissingAnalyses() {
		for (RugbyMatch match : rugbyMatchRepository.findByCompetitionCode("TOP14")) {
			if (match.getAnalysis() != null) {
				continue;
			}
			String home = match.getHomeTeam().getShortName();
			String away = match.getAwayTeam().getShortName();
			if ("VAN".equals(home) && "UBB".equals(away)) {
				match.setAnalysis(VAN_UBB_ANALYSIS);
			} else if ("ASM".equals(home) && "TOU".equals(away)) {
				match.setAnalysis(ASM_TOU_ANALYSIS);
			}
		}
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
			int awayScore,
			String analysis) {
		RugbyMatch match = new RugbyMatch(
				competition,
				teams.get(home),
				teams.get(away),
				kickoffAt,
				matchday,
				MatchStatus.FINISHED,
				homeScore,
				awayScore);
		match.setAnalysis(analysis);
		return match;
	}

	private static RugbyMatch scheduled(
			Competition competition,
			Map<String, Team> teams,
			String home,
			String away,
			int matchday,
			LocalDateTime kickoffAt,
			String analysis) {
		RugbyMatch match = new RugbyMatch(
				competition,
				teams.get(home),
				teams.get(away),
				kickoffAt,
				matchday,
				MatchStatus.SCHEDULED,
				null,
				null);
		match.setAnalysis(analysis);
		return match;
	}
}
