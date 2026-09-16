package com.ovalytics.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.OffensiveBonusRule;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Component
@Order(2)
public class ProD2DataLoader implements ApplicationRunner {

	private static final LocalDate SEASON_START = LocalDate.of(2026, 8, 1);
	private static final String SEASON = "2026-2027";

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;
	private final TransferRepository transferRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public ProD2DataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			PlayerRepository playerRepository,
			TransferRepository transferRepository,
			RugbyMatchRepository rugbyMatchRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
		this.transferRepository = transferRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		var existing = competitionRepository.findByCode("PROD2");
		if (existing.isPresent()) {
			Competition proD2 = existing.get();
			proD2.setSeason(SEASON);
			proD2.setSeasonStart(SEASON_START);
			seedTeamsIfNeeded(proD2);
			if (hasImportedSchedule(proD2)) {
				cleanupDemoTransfers(proD2);
				return;
			}
			seedTransfersIfNeeded(proD2);
			return;
		}

		Competition proD2 = competitionRepository.save(
				new Competition(
						"Pro D2",
						"PROD2",
						SEASON,
						SEASON_START,
						5,
						OffensiveBonusRule.TRY_DIFFERENCE,
						3));

		Map<String, Team> teams = seedTeams(proD2);
		seedTransfers(proD2, teams);
	}

	private Map<String, Team> seedTeams(Competition proD2) {
		Map<String, Team> teams = new HashMap<>();
		prod2Teams(proD2).forEach(team -> teams.put(team.getShortName(), teamRepository.save(team)));
		return teams;
	}

	private void seedTeamsIfNeeded(Competition proD2) {
		Map<String, Team> existing = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("PROD2")) {
			existing.put(team.getShortName(), team);
		}
		for (Team team : prod2Teams(proD2)) {
			if (!existing.containsKey(team.getShortName())) {
				teamRepository.save(team);
			}
		}
	}

	private static List<Team> prod2Teams(Competition proD2) {
		return List.of(
				team("Biarritz Olympique", "BIA", "Biarritz", proD2),
				team("RC Nice", "NIC", "Nice", proD2),
				team("RC Angoulême", "ANG", "Angoulême", proD2),
				team("Colomiers Rugby", "COL", "Colomiers", proD2),
				team("AS Béziers", "BEZ", "Béziers", proD2),
				team("US Oyonnax", "OYO", "Oyonnax", proD2),
				team("US Dax", "DAX", "Dax", proD2),
				team("RC Narbonne", "NAR", "Narbonne", proD2),
				team("FC Grenoble", "GRE", "Grenoble", proD2),
				team("SA Aurillac", "AUR", "Aurillac", proD2),
				team("USON Nevers", "NEV", "Nevers", proD2),
				team("US Montauban", "MTB", "Montauban", proD2),
				team("Provence Rugby", "AIX", "Aix-en-Provence", proD2),
				team("SU Agen", "AGE", "Agen", proD2),
				team("CA Brive", "BRI", "Brive", proD2),
				team("Valence Romans Drôme Rugby", "VAL", "Valence", proD2));
	}

	private void seedTransfersIfNeeded(Competition proD2) {
		if (!transferRepository.existsByCompetitionCode("PROD2")) {
			Map<String, Team> teams = new HashMap<>();
			for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("PROD2")) {
				teams.put(team.getShortName(), team);
			}
			seedTransfers(proD2, teams);
		}
	}

	private boolean hasImportedSchedule(Competition competition) {
		LocalDateTime seasonStart = competition.getSeasonStart().atStartOfDay();
		long scheduled = rugbyMatchRepository.countByCompetitionCodeAndStatusSince(
				competition.getCode(),
				MatchStatus.SCHEDULED,
				seasonStart);
		return scheduled >= 20;
	}

	private void cleanupDemoTransfers(Competition proD2) {
		transferRepository.findByCompetitionCodeOrderByTransferDateDesc(proD2.getCode()).stream()
				.filter(t -> t.getPlayer() != null)
				.forEach(transferRepository::delete);
	}

	private void seedTransfers(Competition proD2, Map<String, Team> teams) {
		Player lucas = playerRepository.save(new Player("Pierre Lucas", teams.get("BEZ")));
		Player ortega = playerRepository.save(new Player("Marc Ortega", teams.get("OYO")));
		Player vidal = playerRepository.save(new Player("Hugo Vidal", teams.get("COL")));
		Player morel = playerRepository.save(new Player("Yanis Morel", teams.get("NEV")));

		transferRepository.saveAll(List.of(
				new Transfer(
						proD2,
						lucas,
						lucas.getName(),
						TransferType.JOIN,
						LocalDate.of(2026, 7, 8),
						null,
						teams.get("BEZ"),
						"Grenoble",
						null,
						"2 ans"),
				new Transfer(
						proD2,
						ortega,
						ortega.getName(),
						TransferType.LEAVE,
						LocalDate.of(2026, 6, 20),
						teams.get("OYO"),
						null,
						null,
						"Carcassonne",
						"1 an"),
				new Transfer(
						proD2,
						vidal,
						vidal.getName(),
						TransferType.EXTENSION,
						LocalDate.of(2026, 5, 15),
						teams.get("COL"),
						teams.get("COL"),
						null,
						null,
						"3 ans"),
				new Transfer(
						proD2,
						morel,
						morel.getName(),
						TransferType.LOAN,
						LocalDate.of(2026, 7, 12),
						teams.get("AIX"),
						teams.get("NEV"),
						null,
						null,
						"1 saison")));
	}

	private static Team team(String name, String shortName, String city, Competition competition) {
		return new Team(name, shortName, city, competition);
	}
}
