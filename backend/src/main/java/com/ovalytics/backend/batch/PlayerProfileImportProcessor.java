package com.ovalytics.backend.batch;

import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class PlayerProfileImportProcessor implements ItemProcessor<PlayerProfileCsvRow, Player> {

	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;

	public PlayerProfileImportProcessor(
			TeamRepository teamRepository,
			PlayerRepository playerRepository) {
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	public Player process(PlayerProfileCsvRow row) {
		Team team = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.teamShortName())
				.orElseThrow(() -> new IllegalStateException(
						"Equipe introuvable: " + row.competitionCode() + "/" + row.teamShortName()));

		String name = cleanName(row.playerName());
		return playerRepository.findByTeamIdAndNameIgnoreCase(team.getId(), name)
				.map(player -> enrich(player, row))
				.orElse(null);
	}

	private Player enrich(Player player, PlayerProfileCsvRow row) {
		if (isPresent(row.profileUrl())) {
			player.setAllRugbyProfileUrl(row.profileUrl().trim());
		}
		player.setSeasonMatches(parseInt(row.seasonMatches()));
		player.setSeasonStarts(parseInt(row.seasonStarts()));
		player.setSeasonMinutes(parseInt(row.seasonMinutes()));
		player.setSeasonTries(parseInt(row.seasonTries()));
		player.setSeasonYellowCards(parseInt(row.seasonYellowCards()));
		player.setSeasonRedCards(parseInt(row.seasonRedCards()));

		LocalDate contractEnd = parseDate(row.contractEndDate());
		if (contractEnd != null) {
			player.setContractEndDate(contractEnd);
		}
		if (isPresent(row.careerHistory())) {
			player.setCareerHistory(row.careerHistory().trim());
		}

		return player;
	}

	private static String cleanName(String value) {
		if (value == null) {
			return "";
		}
		String cleaned = value.replace("&#039;", "'").replace("&amp;", "&").trim();
		cleaned = cleaned.replaceAll("\\s*\\(\\d+\\)", "");
		return cleaned.replaceAll("\\s+", " ").trim();
	}

	private static Integer parseInt(String value) {
		if (!isPresent(value)) {
			return null;
		}
		return Integer.valueOf(value.trim());
	}

	private static LocalDate parseDate(String value) {
		if (!isPresent(value)) {
			return null;
		}
		return LocalDate.parse(value.trim());
	}

	private static boolean isPresent(String value) {
		return value != null && !value.isBlank();
	}
}
