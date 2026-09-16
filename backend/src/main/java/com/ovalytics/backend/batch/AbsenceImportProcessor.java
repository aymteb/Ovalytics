package com.ovalytics.backend.batch;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.domain.AbsenceType;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class AbsenceImportProcessor implements ItemProcessor<AbsenceCsvRow, Absence> {

	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;

	public AbsenceImportProcessor(
			TeamRepository teamRepository,
			PlayerRepository playerRepository) {
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	public Absence process(AbsenceCsvRow row) {
		if (row.playerName() == null || row.playerName().isBlank()) {
			return null;
		}
		Team team = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.teamShortName())
				.orElseThrow(() -> new IllegalStateException(
						"Club introuvable: " + row.competitionCode() + "/" + row.teamShortName()));

		String name = cleanName(row.playerName());
		Player player = playerRepository
				.findByTeamIdAndNameIgnoreCase(team.getId(), name)
				.orElseGet(() -> playerRepository.save(new Player(name, team)));

		AbsenceType type = AbsenceType.valueOf(row.type().trim());
		String note = blankToNull(row.note());
		return new Absence(player, type, note);
	}

	private static String cleanName(String value) {
		return value.replace("&#039;", "'").replace("&amp;", "&").trim();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
