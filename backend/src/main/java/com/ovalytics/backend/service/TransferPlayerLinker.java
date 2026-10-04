package com.ovalytics.backend.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Service
public class TransferPlayerLinker {

	private static final Set<TransferType> LINKABLE_TYPES = EnumSet.of(
			TransferType.JOIN,
			TransferType.LEAVE,
			TransferType.LOAN,
			TransferType.EXTENSION);

	private final PlayerRepository playerRepository;
	private final TeamRepository teamRepository;
	private final TransferRepository transferRepository;

	public TransferPlayerLinker(
			PlayerRepository playerRepository,
			TeamRepository teamRepository,
			TransferRepository transferRepository) {
		this.playerRepository = playerRepository;
		this.teamRepository = teamRepository;
		this.transferRepository = transferRepository;
	}

	@Transactional
	public int backfillUnlinked() {
		List<Transfer> transfers = transferRepository.findUnlinkedByTypes(LINKABLE_TYPES);
		int linked = 0;
		for (Transfer transfer : transfers) {
			String competitionCode = transfer.getCompetition().getCode();
			Team fromTeam = transfer.getFromTeam() != null
					? transfer.getFromTeam()
					: resolveTeam(competitionCode, transfer.getFromClubName());
			Team toTeam = transfer.getToTeam() != null
					? transfer.getToTeam()
					: resolveTeam(competitionCode, transfer.getToClubName());
			Player player = resolveOrCreatePlayer(
					transfer.getType(),
					competitionCode,
					fromTeam,
					toTeam,
					transfer.getPlayerName());
			if (player == null) {
				continue;
			}
			transfer.setPlayer(player);
			if (transfer.getFromTeam() == null && fromTeam != null) {
				transfer.setFromTeam(fromTeam);
			}
			if (transfer.getToTeam() == null && toTeam != null) {
				transfer.setToTeam(toTeam);
			}
			linked++;
		}
		return linked;
	}

	public Player resolveOrCreatePlayer(
			TransferType type,
			String competitionCode,
			Team fromTeam,
			Team toTeam,
			String playerName) {
		String name = cleanName(playerName);
		if (name.isBlank()) {
			return null;
		}
		Player existing = findExistingPlayer(competitionCode, fromTeam, toTeam, name);
		if (existing != null) {
			return existing;
		}
		if (type == TransferType.CONTRACT_END || !LINKABLE_TYPES.contains(type)) {
			return null;
		}
		Team team = teamForNewPlayer(type, fromTeam, toTeam);
		if (team == null) {
			return null;
		}
		return playerRepository.save(new Player(name, team));
	}

	public Team resolveTeam(String competitionCode, String club) {
		if (club == null || club.isBlank()) {
			return null;
		}
		String value = club.trim();
		if (value.length() <= 5 && value.equals(value.toUpperCase(Locale.ROOT))) {
			return teamRepository
					.findByCompetitionCodeAndShortName(competitionCode, value)
					.orElse(null);
		}
		String needle = value.toLowerCase(Locale.ROOT);
		return teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode).stream()
				.filter(team -> {
					String teamName = team.getName().toLowerCase(Locale.ROOT);
					return teamName.equals(needle)
							|| team.getShortName().equalsIgnoreCase(value)
							|| teamName.contains(needle)
							|| needle.contains(teamName);
				})
				.findFirst()
				.orElse(null);
	}

	private Player findExistingPlayer(String competitionCode, Team fromTeam, Team toTeam, String name) {
		if (toTeam != null) {
			Optional<Player> player = playerRepository.findByTeamIdAndNameIgnoreCase(toTeam.getId(), name);
			if (player.isPresent()) {
				return player.get();
			}
		}
		if (fromTeam != null) {
			Optional<Player> player = playerRepository.findByTeamIdAndNameIgnoreCase(fromTeam.getId(), name);
			if (player.isPresent()) {
				return player.get();
			}
		}
		var inCompetition = playerRepository.findByCompetitionCodeAndNameIgnoreCase(competitionCode, name);
		if (!inCompetition.isEmpty()) {
			return inCompetition.getFirst();
		}
		return playerRepository.findFirstByNameIgnoreCase(name).orElse(null);
	}

	private static Team teamForNewPlayer(TransferType type, Team fromTeam, Team toTeam) {
		return switch (type) {
			case JOIN, LOAN -> toTeam;
			case LEAVE -> toTeam != null ? toTeam : fromTeam;
			case EXTENSION -> toTeam != null ? toTeam : fromTeam;
			case CONTRACT_END -> null;
		};
	}

	private static String cleanName(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&#039;", "'").replace("&amp;", "&").trim();
	}
}
