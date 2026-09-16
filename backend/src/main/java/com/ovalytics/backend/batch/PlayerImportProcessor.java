package com.ovalytics.backend.batch;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class PlayerImportProcessor implements ItemProcessor<PlayerCsvRow, Player> {

	private static final Map<String, String> POSITION_ALIASES = Map.ofEntries(
			Map.entry("pilier", "Pilier"),
			Map.entry("talonneur", "Talonneur"),
			Map.entry("2ème ligne", "2ème ligne"),
			Map.entry("2eme ligne", "2ème ligne"),
			Map.entry("deuxième ligne", "2ème ligne"),
			Map.entry("deuxieme ligne", "2ème ligne"),
			Map.entry("3ème ligne", "3ème ligne"),
			Map.entry("3eme ligne", "3ème ligne"),
			Map.entry("troisième ligne", "3ème ligne"),
			Map.entry("troisieme ligne", "3ème ligne"),
			Map.entry("mêlée", "Mêlée"),
			Map.entry("melee", "Mêlée"),
			Map.entry("demi de mêlée", "Mêlée"),
			Map.entry("demi de melee", "Mêlée"),
			Map.entry("ouverture", "Ouverture"),
			Map.entry("demi d'ouverture", "Ouverture"),
			Map.entry("centre", "Centre"),
			Map.entry("ailier", "Ailier"),
			Map.entry("arrière", "Arrière"),
			Map.entry("arriere", "Arrière"));

	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;
	private final PlayerImportTracker tracker;

	public PlayerImportProcessor(
			TeamRepository teamRepository,
			PlayerRepository playerRepository,
			PlayerImportTracker tracker) {
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
		this.tracker = tracker;
	}

	@Override
	public Player process(PlayerCsvRow row) {
		Team team = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.teamShortName())
				.orElseThrow(() -> new IllegalStateException(
						"Equipe introuvable: " + row.competitionCode() + "/" + row.teamShortName()));

		String name = cleanName(row.playerName());
		Player player = playerRepository.findByTeamIdAndNameIgnoreCase(team.getId(), name)
				.orElseGet(() -> new Player(name, team));

		player.setName(name);
		player.setTeam(team);
		player.setPosition(normalizePosition(row.position()));
		player.setAge(parseInt(row.age()));
		player.setHeightCm(parseInt(row.heightCm()));
		player.setWeightKg(parseInt(row.weightKg()));
		player.setNationality(blankToNull(row.nationality()));
		player.setContractType(blankToNull(row.contractType()));
		player.setJiffStatus(blankToNull(row.jiffStatus()));
		player.setContractEndDate(parseDate(row.contractEndDate()));

		tracker.mark(team.getId(), name);
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

	private static String normalizePosition(String value) {
		String cleaned = blankToNull(value);
		if (cleaned == null) {
			return null;
		}
		String key = cleaned.toLowerCase(Locale.ROOT);
		return POSITION_ALIASES.getOrDefault(key, cleaned);
	}

	private static Integer parseInt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Integer.valueOf(value.trim());
	}

	private static LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return LocalDate.parse(value.trim());
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
