package com.ovalytics.backend.config;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.OffensiveBonusRule;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Component
@Order(2)
public class ProD2DataLoader implements ApplicationRunner {

	private static final LocalDate SEASON_START = LocalDate.of(2025, 8, 1);

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;
	private final TransferRepository transferRepository;

	public ProD2DataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			PlayerRepository playerRepository,
			TransferRepository transferRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
		this.transferRepository = transferRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		var existing = competitionRepository.findByCode("PROD2");
		if (existing.isPresent()) {
			seedTransfersIfNeeded(existing.get());
			return;
		}

		Competition proD2 = competitionRepository.save(
				new Competition(
						"Pro D2",
						"PROD2",
						"2025-2026",
						SEASON_START,
						5,
						OffensiveBonusRule.TRY_DIFFERENCE,
						3));

		Map<String, Team> teams = new HashMap<>();
		List.of(
				team("AS Béziers", "BEZ", "Béziers", proD2),
				team("US Oyonnax", "OYO", "Oyonnax", proD2),
				team("Colomiers Rugby", "COL", "Colomiers", proD2),
				team("USON Nevers", "NEV", "Nevers", proD2),
				team("Provence Rugby", "AIX", "Aix-en-Provence", proD2),
				team("SO Chambéry", "CHA", "Chambéry", proD2)).forEach(t -> teams.put(t.getShortName(), teamRepository.save(t)));

		seedTransfers(proD2, teams);
	}

	private void seedTransfersIfNeeded(Competition proD2) {
		Map<String, Team> teams = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("PROD2")) {
			teams.put(team.getShortName(), team);
		}
		if (!transferRepository.existsByCompetitionCode("PROD2")) {
			seedTransfers(proD2, teams);
			return;
		}
		boolean missingPlayer = transferRepository
				.findByCompetitionCodeOrderByTransferDateDesc("PROD2")
				.stream()
				.anyMatch(t -> t.getPlayer() == null);
		if (missingPlayer) {
			transferRepository.deleteAll(
					transferRepository.findByCompetitionCodeOrderByTransferDateDesc("PROD2"));
			seedTransfers(proD2, teams);
		}
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
						LocalDate.of(2025, 7, 8),
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
						LocalDate.of(2025, 6, 20),
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
						LocalDate.of(2025, 5, 15),
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
						LocalDate.of(2025, 7, 12),
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
