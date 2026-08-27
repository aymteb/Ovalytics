package com.ovalytics.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.domain.AbsenceType;
import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchAppearance;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.OffensiveBonusRule;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;
import com.ovalytics.backend.repository.AbsenceRepository;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.MatchAppearanceRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Component
@Order(1)
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
	private final TransferRepository transferRepository;
	private final MatchAppearanceRepository matchAppearanceRepository;

	public Top14DataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository,
			PlayerRepository playerRepository,
			AbsenceRepository absenceRepository,
			TransferRepository transferRepository,
			MatchAppearanceRepository matchAppearanceRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.playerRepository = playerRepository;
		this.absenceRepository = absenceRepository;
		this.transferRepository = transferRepository;
		this.matchAppearanceRepository = matchAppearanceRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (competitionRepository.findByCode("TOP14").isPresent()) {
			Competition top14 = competitionRepository.findByCode("TOP14").orElseThrow();
			top14.setSeasonStart(SEASON_START);
			top14.setOffensiveBonusRule(OffensiveBonusRule.TRY_DIFFERENCE);
			top14.setOffensiveBonusThreshold(3);
			dedupePlayersIfNeeded();
			fillMissingAnalyses();
			fillMissingTries();
			seedPreviousSeasonIfNeeded(top14);
			seedAbsencesIfNeeded();
			seedTransfersIfNeeded(top14);
			fillPlayerProfilesIfNeeded();
			fillContractEndDatesIfNeeded();
			seedAppearancesIfNeeded();
			seedSquadIfNeeded();
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
		seedTransfers(top14, teams);
		seedAppearances();
		seedSquad(teams);
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
		Player vanPillar = ensurePlayer(
				"Maxime Lafage", teams.get("VAN"), "Demi d'ouverture", 28, 178, 86, "France",
				LocalDate.of(2027, 6, 30));
		Player vanBack = ensurePlayer(
				"Romaric Camou", teams.get("VAN"), "Ailier", 26, 182, 88, "France",
				LocalDate.of(2026, 6, 30));
		Player ubbLock = ensurePlayer(
				"Adam Coleman", teams.get("UBB"), "Deuxième ligne", 34, 204, 122, "Australie",
				LocalDate.of(2026, 6, 30));
		Player ubbWing = ensurePlayer(
				"Louis Bielle-Biarrey", teams.get("UBB"), "Ailier", 22, 184, 84, "France",
				LocalDate.of(2028, 6, 30));

		absenceRepository.saveAll(List.of(
				new Absence(vanPillar, AbsenceType.INJURED, "Genou"),
				new Absence(vanBack, AbsenceType.SUSPENDED, "Carton rouge"),
				new Absence(ubbLock, AbsenceType.INJURED, "Epaule"),
				new Absence(ubbWing, AbsenceType.INJURED, "Hamstring")));
	}

	private void seedTransfersIfNeeded(Competition top14) {
		Map<String, Team> teams = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("TOP14")) {
			teams.put(team.getShortName(), team);
		}
		if (!transferRepository.existsByCompetitionCode("TOP14")) {
			seedTransfers(top14, teams);
			return;
		}
		boolean needsReseed = transferRepository
				.findByCompetitionCodeOrderByTransferDateDesc("TOP14")
				.stream()
				.anyMatch(t -> t.getContractLength() == null
						|| t.getContractLength().isBlank()
						|| t.getPlayer() == null);
		if (needsReseed) {
			transferRepository.deleteAll(
					transferRepository.findByCompetitionCodeOrderByTransferDateDesc("TOP14"));
			seedTransfers(top14, teams);
		}
	}

	private void seedTransfers(Competition top14, Map<String, Team> teams) {
		Player willis = ensurePlayer(
				"Jack Willis", teams.get("TOU"), "Troisième ligne", 29, 191, 112, "Angleterre",
				LocalDate.of(2028, 7, 1));
		Player dupont = ensurePlayer(
				"Antoine Dupont", teams.get("TOU"), "Demi de mêlée", 27, 174, 85, "France",
				LocalDate.of(2029, 6, 30));
		Player seuteni = ensurePlayer(
				"Ulupano Seuteni", teams.get("ASM"), "Centre", 31, 185, 100, "Samoa",
				LocalDate.of(2027, 6, 30));
		Player bekri = ensurePlayer(
				"Bekri", teams.get("VAN"), "Pilier", 21, 185, 115, "France",
				LocalDate.of(2026, 7, 15));
		Player woki = ensurePlayer(
				"Cameron Woki", teams.get("UBB"), "Deuxième ligne", 26, 196, 110, "France",
				LocalDate.of(2028, 7, 10));
		Player ramos = ensurePlayer(
				"Thomas Ramos", teams.get("TOU"), "Arrière", 29, 178, 87, "France",
				LocalDate.of(2028, 5, 31));
		Player plisson = ensurePlayer(
				"Jules Plisson", teams.get("VAN"), "Demi d'ouverture", 31, 183, 92, "France",
				LocalDate.of(2025, 6, 30));

		transferRepository.saveAll(List.of(
				new Transfer(
						top14,
						willis,
						willis.getName(),
						TransferType.JOIN,
						LocalDate.of(2025, 7, 2),
						null,
						teams.get("TOU"),
						"Wasps",
						null,
						"3 ans"),
				new Transfer(
						top14,
						dupont,
						dupont.getName(),
						TransferType.EXTENSION,
						LocalDate.of(2025, 6, 18),
						teams.get("TOU"),
						teams.get("TOU"),
						null,
						null,
						"4 ans"),
				new Transfer(
						top14,
						seuteni,
						seuteni.getName(),
						TransferType.LEAVE,
						LocalDate.of(2025, 6, 5),
						teams.get("ASM"),
						null,
						null,
						"Sale Sharks",
						"2 ans"),
				new Transfer(
						top14,
						bekri,
						bekri.getName(),
						TransferType.LOAN,
						LocalDate.of(2025, 7, 15),
						teams.get("TOU"),
						teams.get("VAN"),
						null,
						null,
						"1 saison"),
				new Transfer(
						top14,
						woki,
						woki.getName(),
						TransferType.JOIN,
						LocalDate.of(2025, 7, 10),
						teams.get("RAC"),
						teams.get("UBB"),
						null,
						null,
						"3 ans"),
				new Transfer(
						top14,
						ramos,
						ramos.getName(),
						TransferType.EXTENSION,
						LocalDate.of(2025, 5, 28),
						teams.get("TOU"),
						teams.get("TOU"),
						null,
						null,
						"3 ans"),
				new Transfer(
						top14,
						plisson,
						plisson.getName(),
						TransferType.CONTRACT_END,
						LocalDate.of(2025, 6, 30),
						teams.get("VAN"),
						null,
						null,
						null,
						null)));
	}

	private void fillPlayerProfilesIfNeeded() {
		fillProfile("Antoine Dupont", "Demi de mêlée", 27, 174, 85, "France");
		fillProfile("Thomas Ramos", "Arrière", 29, 178, 87, "France");
		fillProfile("Cameron Woki", "Deuxième ligne", 26, 196, 110, "France");
		fillProfile("Jack Willis", "Troisième ligne", 29, 191, 112, "Angleterre");
		fillProfile("Ulupano Seuteni", "Centre", 31, 185, 100, "Samoa");
		fillProfile("Bekri", "Pilier", 21, 185, 115, "France");
		fillProfile("Jules Plisson", "Demi d'ouverture", 31, 183, 92, "France");
		fillProfile("Maxime Lafage", "Demi d'ouverture", 28, 178, 86, "France");
		fillProfile("Romaric Camou", "Ailier", 26, 182, 88, "France");
		fillProfile("Adam Coleman", "Deuxième ligne", 34, 204, 122, "Australie");
		fillProfile("Louis Bielle-Biarrey", "Ailier", 22, 184, 84, "France");
	}

	private void fillProfile(
			String name,
			String position,
			int age,
			int heightCm,
			int weightKg,
			String nationality) {
		playerRepository.findFirstByName(name).ifPresent(player -> {
			if (player.getPosition() == null || player.getPosition().isBlank()) {
				player.setPosition(position);
				player.setAge(age);
				player.setHeightCm(heightCm);
				player.setWeightKg(weightKg);
				player.setNationality(nationality);
			}
		});
	}

	private void seedAppearancesIfNeeded() {
		if (matchAppearanceRepository.count() > 0) {
			return;
		}
		seedAppearances();
	}

	private void seedSquadIfNeeded() {
		Map<String, Team> teams = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("TOP14")) {
			teams.put(team.getShortName(), team);
		}
		seedSquad(teams);
	}

	private void seedSquad(Map<String, Team> teams) {
		ensurePlayer("Cyril Baille", teams.get("TOU"), "Pilier", 31, 182, 116, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Julien Marchand", teams.get("TOU"), "Talonneur", 30, 180, 108, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Dorian Aldegheri", teams.get("TOU"), "Pilier", 31, 180, 120, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Emmanuel Meafou", teams.get("TOU"), "Deuxième ligne", 26, 203, 145, "Australie", LocalDate.of(2029, 6, 30));
		ensurePlayer("François Cros", teams.get("TOU"), "Troisième ligne", 30, 190, 108, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Romain Ntamack", teams.get("TOU"), "Demi d'ouverture", 25, 186, 91, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Pita Ahki", teams.get("TOU"), "Centre", 32, 185, 100, "Nouvelle-Zélande", LocalDate.of(2026, 6, 30));
		ensurePlayer("Juan Cruz Mallia", teams.get("TOU"), "Ailier", 28, 182, 90, "Argentine", LocalDate.of(2027, 6, 30));

		ensurePlayer("Jefferson Poirot", teams.get("UBB"), "Pilier", 32, 183, 116, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Maxime Lamothe", teams.get("UBB"), "Talonneur", 26, 182, 105, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Ugo Boniface", teams.get("UBB"), "Pilier", 26, 185, 122, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Kane Douglas", teams.get("UBB"), "Deuxième ligne", 35, 201, 120, "Australie", LocalDate.of(2026, 6, 30));
		ensurePlayer("Mahamadou Diaby", teams.get("UBB"), "Troisième ligne", 30, 192, 110, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Maxime Lucu", teams.get("UBB"), "Demi de mêlée", 31, 177, 85, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Matthieu Jalibert", teams.get("UBB"), "Demi d'ouverture", 26, 182, 86, "France", LocalDate.of(2028, 6, 30));
		ensurePlayer("Yoram Moefana", teams.get("UBB"), "Centre", 24, 183, 95, "France", LocalDate.of(2027, 6, 30));
		ensurePlayer("Damian Penaud", teams.get("UBB"), "Ailier", 28, 188, 95, "France", LocalDate.of(2029, 6, 30));
		ensurePlayer("Romain Buros", teams.get("UBB"), "Arrière", 27, 185, 90, "France", LocalDate.of(2028, 6, 30));
	}

	private Player ensurePlayer(
			String name,
			Team team,
			String position,
			Integer age,
			Integer heightCm,
			Integer weightKg,
			String nationality,
			LocalDate contractEndDate) {
		if (team == null) {
			return null;
		}
		return playerRepository.findByTeamIdAndName(team.getId(), name)
				.map(player -> enrichPlayer(
						player, position, age, heightCm, weightKg, nationality, contractEndDate))
				.orElseGet(() -> playerRepository.save(new Player(
						name,
						team,
						position,
						age,
						heightCm,
						weightKg,
						nationality,
						contractEndDate)));
	}

	private Player enrichPlayer(
			Player player,
			String position,
			Integer age,
			Integer heightCm,
			Integer weightKg,
			String nationality,
			LocalDate contractEndDate) {
		if (player.getPosition() == null || player.getPosition().isBlank()) {
			player.setPosition(position);
		}
		if (player.getAge() == null && age != null) {
			player.setAge(age);
		}
		if (player.getHeightCm() == null && heightCm != null) {
			player.setHeightCm(heightCm);
		}
		if (player.getWeightKg() == null && weightKg != null) {
			player.setWeightKg(weightKg);
		}
		if (player.getNationality() == null || player.getNationality().isBlank()) {
			player.setNationality(nationality);
		}
		if (player.getContractEndDate() == null && contractEndDate != null) {
			player.setContractEndDate(contractEndDate);
		}
		return player;
	}

	private void dedupePlayersIfNeeded() {
		Map<String, List<Player>> groups = new HashMap<>();
		for (Player player : playerRepository.findAll()) {
			String key = player.getTeam().getId() + "::" + player.getName();
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
		}
		for (List<Player> group : groups.values()) {
			if (group.size() <= 1) {
				continue;
			}
			group.sort(Comparator.comparing(Player::getId));
			Player keep = group.get(0);
			for (int i = 1; i < group.size(); i++) {
				mergePlayerInto(keep, group.get(i));
				playerRepository.delete(group.get(i));
			}
		}
	}

	private void mergePlayerInto(Player keep, Player duplicate) {
		for (Transfer transfer : transferRepository.findByPlayerIdOrderByTransferDateDesc(duplicate.getId())) {
			transfer.setPlayer(keep);
		}
		for (MatchAppearance appearance : matchAppearanceRepository
				.findByPlayerIdOrderByKickoffDesc(duplicate.getId())) {
			if (matchAppearanceRepository
					.findByPlayerIdAndMatchId(keep.getId(), appearance.getMatch().getId())
					.isPresent()) {
				matchAppearanceRepository.delete(appearance);
			} else {
				appearance.setPlayer(keep);
			}
		}
		for (Absence absence : absenceRepository.findByPlayerId(duplicate.getId())) {
			absence.setPlayer(keep);
		}
	}

	private void fillContractEndDatesIfNeeded() {
		fillContractEnd("Antoine Dupont", "TOU", LocalDate.of(2029, 6, 30));
		fillContractEnd("Thomas Ramos", "TOU", LocalDate.of(2028, 5, 31));
		fillContractEnd("Jack Willis", "TOU", LocalDate.of(2028, 7, 1));
		fillContractEnd("Cameron Woki", "UBB", LocalDate.of(2028, 7, 10));
		fillContractEnd("Bekri", "VAN", LocalDate.of(2026, 7, 15));
		fillContractEnd("Louis Bielle-Biarrey", "UBB", LocalDate.of(2028, 6, 30));
		fillContractEnd("Adam Coleman", "UBB", LocalDate.of(2026, 6, 30));
	}

	private void fillContractEnd(String name, String shortName, LocalDate contractEndDate) {
		teamRepository.findByCompetitionCodeAndShortName("TOP14", shortName)
				.flatMap(team -> playerRepository.findByTeamIdAndName(team.getId(), name))
				.ifPresent(player -> {
					if (player.getContractEndDate() == null) {
						player.setContractEndDate(contractEndDate);
					}
				});
	}

	private void seedAppearances() {
		Player dupont = playerOnTeam("TOU", "Antoine Dupont");
		Player ramos = playerOnTeam("TOU", "Thomas Ramos");
		Player woki = playerOnTeam("UBB", "Cameron Woki");
		if (dupont == null && ramos == null && woki == null) {
			return;
		}

		List<MatchAppearance> appearances = new ArrayList<>();
		addAppearance(appearances, dupont, "TOU", "VAN", 1, 9, true, 72, 1, 0, 0);
		addAppearance(appearances, dupont, "TOU", "UBB", 22, 9, true, 80, 0, 0, 0);
		addAppearance(appearances, dupont, "TOL", "TOU", 23, 9, true, 65, 1, 1, 0);
		addAppearance(appearances, dupont, "TOU", "LAR", 24, 9, true, 80, 0, 0, 0);
		addAppearance(appearances, ramos, "TOU", "VAN", 1, 15, true, 80, 0, 0, 0);
		addAppearance(appearances, ramos, "TOU", "UBB", 22, 15, true, 80, 1, 0, 0);
		addAppearance(appearances, ramos, "TOL", "TOU", 23, 15, true, 74, 0, 0, 0);
		addAppearance(appearances, woki, "UBB", "ASM", 1, 4, true, 70, 0, 0, 0);
		addAppearance(appearances, woki, "TOU", "UBB", 22, 4, true, 68, 0, 1, 0);
		addAppearance(appearances, woki, "UBB", "LAR", 23, 19, false, 28, 0, 0, 0);
		addAppearance(appearances, woki, "TOL", "UBB", 25, 4, true, 75, 0, 0, 0);

		if (!appearances.isEmpty()) {
			matchAppearanceRepository.saveAll(appearances);
		}
	}

	private Player playerOnTeam(String shortName, String name) {
		return teamRepository.findByCompetitionCodeAndShortName("TOP14", shortName)
				.flatMap(team -> playerRepository.findByTeamIdAndName(team.getId(), name))
				.orElse(null);
	}

	private void addAppearance(
			List<MatchAppearance> appearances,
			Player player,
			String home,
			String away,
			int matchday,
			int jerseyNumber,
			boolean starter,
			int minutes,
			int tries,
			int yellowCards,
			int redCards) {
		if (player == null) {
			return;
		}
		rugbyMatchRepository
				.findByCompetitionAndTeamsAndMatchday("TOP14", home, away, matchday)
				.ifPresent(match -> appearances.add(new MatchAppearance(
						player,
						match,
						jerseyNumber,
						starter,
						minutes,
						tries,
						yellowCards,
						redCards)));
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
