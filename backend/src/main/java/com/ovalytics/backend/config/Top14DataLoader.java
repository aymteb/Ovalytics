package com.ovalytics.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.domain.AbsenceType;
import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.OffensiveBonusRule;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.AbsenceRepository;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class Top14DataLoader implements ApplicationRunner {

	private static final LocalDate SEASON_START = LocalDate.of(2025, 8, 1);

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
	private final PlayerRepository playerRepository;
	private final AbsenceRepository absenceRepository;

	public Top14DataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository,
			PlayerRepository playerRepository,
			AbsenceRepository absenceRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.playerRepository = playerRepository;
		this.absenceRepository = absenceRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (competitionRepository.findByCode("TOP14").isPresent()) {
			Competition top14 = competitionRepository.findByCode("TOP14").orElseThrow();
			top14.setSeasonStart(SEASON_START);
			top14.setOffensiveBonusRule(OffensiveBonusRule.TRY_DIFFERENCE);
			top14.setOffensiveBonusThreshold(3);
			fillMissingAnalyses();
			fillMissingTries();
			seedPreviousSeasonIfNeeded(top14);
			seedAbsencesIfNeeded();
			return;
		}

		Competition top14 = competitionRepository.save(
				new Competition(
						"Top 14",
						"TOP14",
						"2025-2026",
						SEASON_START,
						5,
						OffensiveBonusRule.TRY_DIFFERENCE,
						3));

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

		rugbyMatchRepository.saveAll(previousSeasonMatches(top14, teams));

		rugbyMatchRepository.saveAll(List.of(
				finished(top14, teams, "TOU", "VAN", 1, LocalDateTime.of(2025, 9, 6, 21, 5), 42, 14, 6, 2, null),
				finished(top14, teams, "UBB", "ASM", 1, LocalDateTime.of(2025, 9, 6, 16, 0), 28, 21, 4, 3, null),
				finished(top14, teams, "LAR", "CAS", 1, LocalDateTime.of(2025, 9, 6, 18, 30), 24, 20, 3, 2, null),
				finished(top14, teams, "TOL", "BAY", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 33, 19, 5, 2, null),
				finished(top14, teams, "SFP", "PAU", 1, LocalDateTime.of(2025, 9, 7, 17, 0), 27, 27, 3, 3, null),
				finished(top14, teams, "RAC", "MHR", 1, LocalDateTime.of(2025, 9, 7, 15, 0), 31, 17, 4, 2, null),
				finished(top14, teams, "LOU", "USAP", 1, LocalDateTime.of(2025, 9, 7, 21, 5), 22, 16, 2, 2, null),
				scheduled(top14, teams, "VAN", "UBB", 2, LocalDateTime.of(2025, 9, 13, 16, 0), VAN_UBB_ANALYSIS),
				scheduled(top14, teams, "ASM", "TOU", 2, LocalDateTime.of(2025, 9, 13, 21, 5), ASM_TOU_ANALYSIS),
				scheduled(top14, teams, "CAS", "TOL", 2, LocalDateTime.of(2025, 9, 14, 17, 0), null),
				scheduled(top14, teams, "BAY", "LAR", 2, LocalDateTime.of(2025, 9, 14, 21, 5), null),
				scheduled(top14, teams, "PAU", "RAC", 2, LocalDateTime.of(2025, 9, 14, 15, 0), null),
				scheduled(top14, teams, "MHR", "SFP", 2, LocalDateTime.of(2025, 9, 14, 18, 30), null),
				scheduled(top14, teams, "USAP", "LOU", 2, LocalDateTime.of(2025, 9, 13, 18, 30), null)));

		seedAbsences(teams);
	}

	private void seedPreviousSeasonIfNeeded(Competition top14) {
		LocalDateTime seasonStart = top14.getSeasonStart().atStartOfDay();
		boolean alreadySeeded = rugbyMatchRepository.findByCompetitionCode("TOP14").stream()
				.anyMatch(m -> m.getKickoffAt().isBefore(seasonStart));
		if (alreadySeeded) {
			return;
		}
		Map<String, Team> teams = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("TOP14")) {
			teams.put(team.getShortName(), team);
		}
		rugbyMatchRepository.saveAll(previousSeasonMatches(top14, teams));
	}

	private static List<RugbyMatch> previousSeasonMatches(Competition top14, Map<String, Team> teams) {
		return List.of(
				finished(top14, teams, "TOU", "UBB", 22, LocalDateTime.of(2025, 5, 10, 21, 5), 35, 21, 5, 3, null),
				finished(top14, teams, "LAR", "TOL", 22, LocalDateTime.of(2025, 5, 10, 16, 0), 19, 26, 2, 4, null),
				finished(top14, teams, "SFP", "RAC", 22, LocalDateTime.of(2025, 5, 10, 18, 30), 24, 24, 3, 3, null),
				finished(top14, teams, "ASM", "LOU", 22, LocalDateTime.of(2025, 5, 11, 21, 5), 28, 17, 4, 2, null),
				finished(top14, teams, "CAS", "MHR", 22, LocalDateTime.of(2025, 5, 11, 15, 0), 22, 16, 3, 2, null),
				finished(top14, teams, "PAU", "BAY", 22, LocalDateTime.of(2025, 5, 11, 17, 0), 20, 23, 2, 3, null),
				finished(top14, teams, "VAN", "USAP", 22, LocalDateTime.of(2025, 5, 11, 21, 5), 27, 20, 4, 2, null),

				finished(top14, teams, "UBB", "LAR", 23, LocalDateTime.of(2025, 5, 17, 21, 5), 31, 14, 5, 2, null),
				finished(top14, teams, "TOL", "TOU", 23, LocalDateTime.of(2025, 5, 17, 16, 0), 18, 29, 2, 4, null),
				finished(top14, teams, "RAC", "ASM", 23, LocalDateTime.of(2025, 5, 17, 18, 30), 25, 22, 3, 3, null),
				finished(top14, teams, "LOU", "SFP", 23, LocalDateTime.of(2025, 5, 18, 21, 5), 16, 21, 2, 3, null),
				finished(top14, teams, "MHR", "PAU", 23, LocalDateTime.of(2025, 5, 18, 15, 0), 33, 19, 5, 2, null),
				finished(top14, teams, "BAY", "CAS", 23, LocalDateTime.of(2025, 5, 18, 17, 0), 27, 24, 3, 3, null),
				finished(top14, teams, "USAP", "VAN", 23, LocalDateTime.of(2025, 5, 18, 21, 5), 14, 28, 2, 4, null),

				finished(top14, teams, "TOU", "LAR", 24, LocalDateTime.of(2025, 5, 24, 21, 5), 38, 17, 6, 2, null),
				finished(top14, teams, "UBB", "TOL", 24, LocalDateTime.of(2025, 5, 24, 16, 0), 26, 26, 3, 3, null),
				finished(top14, teams, "SFP", "ASM", 24, LocalDateTime.of(2025, 5, 24, 18, 30), 21, 18, 3, 2, null),
				finished(top14, teams, "RAC", "LOU", 24, LocalDateTime.of(2025, 5, 25, 21, 5), 29, 20, 4, 2, null),
				finished(top14, teams, "CAS", "PAU", 24, LocalDateTime.of(2025, 5, 25, 15, 0), 24, 15, 3, 2, null),
				finished(top14, teams, "MHR", "BAY", 24, LocalDateTime.of(2025, 5, 25, 17, 0), 19, 27, 2, 4, null),
				finished(top14, teams, "VAN", "USAP", 24, LocalDateTime.of(2025, 5, 25, 21, 5), 22, 22, 3, 3, null),

				finished(top14, teams, "LAR", "TOU", 25, LocalDateTime.of(2025, 5, 31, 21, 5), 20, 34, 2, 5, null),
				finished(top14, teams, "TOL", "UBB", 25, LocalDateTime.of(2025, 5, 31, 16, 0), 23, 28, 3, 4, null),
				finished(top14, teams, "ASM", "SFP", 25, LocalDateTime.of(2025, 5, 31, 18, 30), 27, 19, 4, 2, null),
				finished(top14, teams, "LOU", "RAC", 25, LocalDateTime.of(2025, 6, 1, 21, 5), 18, 24, 2, 3, null),
				finished(top14, teams, "PAU", "CAS", 25, LocalDateTime.of(2025, 6, 1, 15, 0), 16, 21, 2, 3, null),
				finished(top14, teams, "BAY", "MHR", 25, LocalDateTime.of(2025, 6, 1, 17, 0), 30, 17, 4, 2, null),
				finished(top14, teams, "USAP", "VAN", 25, LocalDateTime.of(2025, 6, 1, 21, 5), 21, 26, 3, 3, null),

				finished(top14, teams, "TOU", "SFP", 26, LocalDateTime.of(2025, 6, 7, 21, 5), 41, 14, 6, 2, null),
				finished(top14, teams, "UBB", "RAC", 26, LocalDateTime.of(2025, 6, 7, 16, 0), 28, 22, 4, 3, null),
				finished(top14, teams, "LAR", "ASM", 26, LocalDateTime.of(2025, 6, 7, 18, 30), 24, 21, 3, 3, null),
				finished(top14, teams, "TOL", "LOU", 26, LocalDateTime.of(2025, 6, 8, 21, 5), 31, 18, 5, 2, null),
				finished(top14, teams, "CAS", "BAY", 26, LocalDateTime.of(2025, 6, 8, 15, 0), 19, 19, 2, 2, null),
				finished(top14, teams, "MHR", "USAP", 26, LocalDateTime.of(2025, 6, 8, 17, 0), 25, 20, 3, 2, null),
				finished(top14, teams, "PAU", "VAN", 26, LocalDateTime.of(2025, 6, 8, 21, 5), 17, 23, 2, 3, null));
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

	private void fillMissingTries() {
		for (RugbyMatch match : rugbyMatchRepository.findByCompetitionCodeAndStatus(
				"TOP14", MatchStatus.FINISHED)) {
			if (match.getHomeTries() != null) {
				continue;
			}
			String home = match.getHomeTeam().getShortName();
			String away = match.getAwayTeam().getShortName();
			int[] tries = demoTries(home, away, match.getMatchday());
			if (tries != null) {
				match.setHomeTries(tries[0]);
				match.setAwayTries(tries[1]);
			}
		}
	}

	private static int[] demoTries(String home, String away, int matchday) {
		if (matchday != 1) {
			return null;
		}
		return switch (home + "-" + away) {
			case "TOU-VAN" -> new int[] {6, 2};
			case "UBB-ASM" -> new int[] {4, 3};
			case "LAR-CAS" -> new int[] {3, 2};
			case "TOL-BAY" -> new int[] {5, 2};
			case "SFP-PAU" -> new int[] {3, 3};
			case "RAC-MHR" -> new int[] {4, 2};
			case "LOU-USAP" -> new int[] {2, 2};
			default -> null;
		};
	}

	private void seedAbsencesIfNeeded() {
		if (playerRepository.count() > 0) {
			return;
		}
		Map<String, Team> teams = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("TOP14")) {
			teams.put(team.getShortName(), team);
		}
		seedAbsences(teams);
	}

	private void seedAbsences(Map<String, Team> teams) {
		Player vanPillar = playerRepository.save(new Player("Maxime Lafage", teams.get("VAN")));
		Player vanBack = playerRepository.save(new Player("Romaric Camou", teams.get("VAN")));
		Player ubbLock = playerRepository.save(new Player("Adam Coleman", teams.get("UBB")));
		Player ubbWing = playerRepository.save(new Player("Louis Bielle-Biarrey", teams.get("UBB")));

		absenceRepository.saveAll(List.of(
				new Absence(vanPillar, AbsenceType.INJURED, "Genou"),
				new Absence(vanBack, AbsenceType.SUSPENDED, "Carton rouge"),
				new Absence(ubbLock, AbsenceType.INJURED, "Epaule"),
				new Absence(ubbWing, AbsenceType.INJURED, "Hamstring")));
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
			int homeTries,
			int awayTries,
			String analysis) {
		RugbyMatch match = new RugbyMatch(
				competition,
				teams.get(home),
				teams.get(away),
				kickoffAt,
				matchday,
				MatchStatus.FINISHED,
				homeScore,
				awayScore,
				homeTries,
				awayTries);
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
				null,
				null,
				null);
		match.setAnalysis(analysis);
		return match;
	}
}
