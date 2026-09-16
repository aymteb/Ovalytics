package com.ovalytics.backend.batch;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.domain.TransferType;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Component
public class TransferImportProcessor implements ItemProcessor<TransferCsvRow, Transfer> {

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final TransferRepository transferRepository;
	private final PlayerRepository playerRepository;

	public TransferImportProcessor(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			TransferRepository transferRepository,
			PlayerRepository playerRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.transferRepository = transferRepository;
		this.playerRepository = playerRepository;
	}

	@Override
	public Transfer process(TransferCsvRow row) {
		Competition competition = competitionRepository.findByCode(row.competitionCode())
				.orElseThrow(() -> new IllegalStateException(
						"Competition introuvable: " + row.competitionCode()));

		TransferType type = TransferType.valueOf(row.type());
		LocalDate transferDate = LocalDate.parse(row.transferDate());
		String fromClubKey = clubKey(row.fromClub());
		String toClubKey = clubKey(row.toClub());
		Team fromTeam = resolveTeam(row.competitionCode(), row.fromClub());
		Team toTeam = resolveTeam(row.competitionCode(), row.toClub());
		String contractLength = blankToNull(row.contractLength());
		Player player = resolvePlayer(fromTeam, toTeam, row.playerName());

		return findExisting(row.competitionCode(), row.playerName(), transferDate, type, fromClubKey, toClubKey)
				.map(existing -> {
					existing.setFromTeam(fromTeam);
					existing.setToTeam(toTeam);
					existing.setFromClubName(fromClubKey);
					existing.setToClubName(toClubKey);
					existing.setContractLength(contractLength);
					if (player != null) {
						existing.setPlayer(player);
					}
					return existing;
				})
				.orElseGet(() -> new Transfer(
						competition,
						player,
						row.playerName(),
						type,
						transferDate,
						fromTeam,
						toTeam,
						fromClubKey,
						toClubKey,
						contractLength));
	}

	private Optional<Transfer> findExisting(
			String competitionCode,
			String playerName,
			LocalDate transferDate,
			TransferType type,
			String fromClubKey,
			String toClubKey) {
		Optional<Transfer> byClubs = transferRepository
				.findByCompetitionCodeAndPlayerNameAndTransferDateAndTypeAndClubs(
						competitionCode,
						playerName,
						transferDate,
						type,
						fromClubKey,
						toClubKey);
		if (byClubs.isPresent()) {
			return byClubs;
		}
		return transferRepository
				.findByCompetitionCodeAndPlayerNameAndTransferDateAndType(
						competitionCode,
						playerName,
						transferDate,
						type)
				.filter(existing -> matchesLegacyImport(existing, fromClubKey, toClubKey));
	}

	private boolean matchesLegacyImport(Transfer existing, String fromClubKey, String toClubKey) {
		return clubKeyFromTransfer(existing, true).equals(fromClubKey)
				&& clubKeyFromTransfer(existing, false).equals(toClubKey);
	}

	private String clubKeyFromTransfer(Transfer transfer, boolean from) {
		Team team = from ? transfer.getFromTeam() : transfer.getToTeam();
		String clubName = from ? transfer.getFromClubName() : transfer.getToClubName();
		if (clubName != null && !clubName.isBlank()) {
			return clubName;
		}
		if (team != null) {
			return team.getShortName();
		}
		return "";
	}

	private Team resolveTeam(String competitionCode, String club) {
		if (club == null || club.isBlank()) {
			return null;
		}
		String value = club.trim();
		if (value.length() <= 5 && value.equals(value.toUpperCase(Locale.ROOT))) {
			return teamRepository
					.findByCompetitionCodeAndShortName(competitionCode, value)
					.orElse(null);
		}
		return teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode).stream()
				.filter(team -> team.getName().equalsIgnoreCase(value)
						|| team.getShortName().equalsIgnoreCase(value))
				.findFirst()
				.orElse(null);
	}

	private Player resolvePlayer(Team fromTeam, Team toTeam, String playerName) {
		String name = cleanName(playerName);
		if (toTeam != null) {
			Optional<Player> player = playerRepository.findByTeamIdAndNameIgnoreCase(toTeam.getId(), name);
			if (player.isPresent()) {
				return player.get();
			}
		}
		if (fromTeam != null) {
			return playerRepository.findByTeamIdAndNameIgnoreCase(fromTeam.getId(), name).orElse(null);
		}
		return null;
	}

	private static String cleanName(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&#039;", "'").replace("&amp;", "&").trim();
	}

	private static String clubKey(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.trim();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
