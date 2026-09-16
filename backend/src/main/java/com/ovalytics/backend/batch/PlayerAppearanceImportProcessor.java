package com.ovalytics.backend.batch;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.MatchAppearance;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.MatchAppearanceRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class PlayerAppearanceImportProcessor implements ItemProcessor<PlayerAppearanceCsvRow, MatchAppearance> {

	private final TeamRepository teamRepository;
	private final PlayerRepository playerRepository;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final MatchAppearanceRepository matchAppearanceRepository;

	public PlayerAppearanceImportProcessor(
			TeamRepository teamRepository,
			PlayerRepository playerRepository,
			RugbyMatchRepository rugbyMatchRepository,
			MatchAppearanceRepository matchAppearanceRepository) {
		this.teamRepository = teamRepository;
		this.playerRepository = playerRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.matchAppearanceRepository = matchAppearanceRepository;
	}

	@Override
	public MatchAppearance process(PlayerAppearanceCsvRow row) {
		Team team = teamRepository
				.findByCompetitionCodeAndShortName(row.competitionCode(), row.teamShortName())
				.orElse(null);
		if (team == null) {
			return null;
		}

		Player player = playerRepository
				.findByTeamIdAndNameIgnoreCase(team.getId(), cleanName(row.playerName()))
				.orElse(null);
		if (player == null) {
			return null;
		}

		RugbyMatch match = rugbyMatchRepository
				.findByCompetitionAndTeamsAndMatchday(
						row.competitionCode(),
						row.homeShortName(),
						row.awayShortName(),
						row.matchday())
				.orElse(null);
		if (match == null) {
			return null;
		}

		if (matchAppearanceRepository.findByPlayerIdAndMatchId(player.getId(), match.getId()).isPresent()) {
			return null;
		}

		int jerseyNumber = parseInt(row.jerseyNumber(), 0);
		int minutesPlayed = parseInt(row.minutesPlayed(), 0);
		boolean starter = minutesPlayed >= 60;
		int tries = parseInt(row.tries(), 0);
		int yellowCards = parseInt(row.yellowCards(), 0);
		int redCards = parseInt(row.redCards(), 0);

		return new MatchAppearance(
				player,
				match,
				jerseyNumber,
				starter,
				minutesPlayed,
				tries,
				yellowCards,
				redCards);
	}

	private static String cleanName(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&#039;", "'").replace("&amp;", "&").trim();
	}

	private static int parseInt(String value, int fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}
}
