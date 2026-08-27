package com.ovalytics.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ovalytics.backend.domain.Absence;
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
import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.ClubMercatoResponse;
import com.ovalytics.backend.web.dto.CompetitionResponse;
import com.ovalytics.backend.web.dto.HeadToHeadMatchResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.PlayerAppearanceResponse;
import com.ovalytics.backend.web.dto.PlayerDetailResponse;
import com.ovalytics.backend.web.dto.PlayerTotalsResponse;
import com.ovalytics.backend.web.dto.SquadPlayerResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;
import com.ovalytics.backend.web.dto.TeamResponse;
import com.ovalytics.backend.web.dto.TransferResponse;
import com.ovalytics.backend.web.dto.VenueRecordResponse;

@Service
public class CompetitionQueryService {

	private static final int FORM_SIZE = 5;
	private static final int HEAD_TO_HEAD_SIZE = 5;

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final AbsenceRepository absenceRepository;
	private final TransferRepository transferRepository;
	private final PlayerRepository playerRepository;
	private final MatchAppearanceRepository matchAppearanceRepository;

	public CompetitionQueryService(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository,
			AbsenceRepository absenceRepository,
			TransferRepository transferRepository,
			PlayerRepository playerRepository,
			MatchAppearanceRepository matchAppearanceRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.absenceRepository = absenceRepository;
		this.transferRepository = transferRepository;
		this.playerRepository = playerRepository;
		this.matchAppearanceRepository = matchAppearanceRepository;
	}

	public List<CompetitionResponse> listCompetitions() {
		return competitionRepository.findAllByOrderByNameAsc().stream()
				.sorted((a, b) -> {
					if ("TOP14".equals(a.getCode())) {
						return -1;
					}
					if ("TOP14".equals(b.getCode())) {
						return 1;
					}
					return a.getName().compareToIgnoreCase(b.getName());
				})
				.map(c -> new CompetitionResponse(c.getId(), c.getName(), c.getCode(), c.getSeason()))
				.toList();
	}

	public List<TeamResponse> listTeams(String competitionCode) {
		ensureCompetitionExists(competitionCode);
		return teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode).stream()
				.map(this::toTeamResponse)
				.toList();
	}

	public List<MatchResponse> listMatches(String competitionCode, MatchStatus status) {
		Competition competition = getCompetition(competitionCode);
		List<RugbyMatch> matches = status == null
				? rugbyMatchRepository.findByCompetitionCode(competitionCode)
				: rugbyMatchRepository.findByCompetitionCodeAndStatus(competitionCode, status);
		if (status == MatchStatus.FINISHED) {
			LocalDateTime seasonStart = competition.getSeasonStart().atStartOfDay();
			matches = matches.stream()
					.filter(m -> !m.getKickoffAt().isBefore(seasonStart))
					.toList();
		}
		return matches.stream()
				.map(match -> toMatchResponse(match, List.of(), List.of(), null, null, null, null, List.of()))
				.toList();
	}

	public MatchResponse getMatch(String competitionCode, Long matchId) {
		Competition competition = getCompetition(competitionCode);
		RugbyMatch match = rugbyMatchRepository
				.findByCompetitionCodeAndId(competitionCode, matchId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Match not found: " + matchId));

		List<RugbyMatch> finished = rugbyMatchRepository
				.findByCompetitionCodeAndStatus(competitionCode, MatchStatus.FINISHED);
		LocalDateTime before = match.getKickoffAt();
		LocalDateTime seasonStart = competition.getSeasonStart().atStartOfDay();
		Long homeId = match.getHomeTeam().getId();
		Long awayId = match.getAwayTeam().getId();

		return toMatchResponse(
				match,
				toAbsenceResponses(absenceRepository.findByTeamId(homeId)),
				toAbsenceResponses(absenceRepository.findByTeamId(awayId)),
				buildForm(finished, homeId, before, seasonStart),
				buildForm(finished, awayId, before, seasonStart),
				buildVenueRecord(finished, homeId, true, before),
				buildVenueRecord(finished, awayId, false, before),
				buildHeadToHead(finished, homeId, awayId, before));
	}

	public List<TransferResponse> listTransfers(String competitionCode) {
		ensureCompetitionExists(competitionCode);
		return transferRepository.findByCompetitionCodeOrderByTransferDateDesc(competitionCode).stream()
				.map(this::toTransferResponse)
				.toList();
	}

	public List<TransferResponse> listAllTransfers() {
		return transferRepository.findAllByOrderByTransferDateDesc().stream()
				.map(this::toTransferResponse)
				.toList();
	}

	public ClubMercatoResponse clubMercato(String competitionCode, String shortName) {
		Competition competition = getCompetition(competitionCode);
		Team team = teamRepository.findByCompetitionCodeAndShortName(competitionCode, shortName)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Club not found: " + shortName));
		List<Transfer> transfers = transferRepository
				.findByCompetitionCodeOrderByTransferDateDesc(competitionCode);

		List<TransferResponse> arrivals = transfers.stream()
				.filter(t -> t.getToTeam() != null && t.getToTeam().getId().equals(team.getId()))
				.filter(t -> t.getType() == TransferType.JOIN || t.getType() == TransferType.LOAN)
				.map(this::toTransferResponse)
				.toList();
		List<TransferResponse> departures = transfers.stream()
				.filter(t -> t.getFromTeam() != null && t.getFromTeam().getId().equals(team.getId()))
				.filter(t -> t.getType() == TransferType.LEAVE
						|| t.getType() == TransferType.LOAN
						|| t.getType() == TransferType.CONTRACT_END)
				.map(this::toTransferResponse)
				.toList();
		List<TransferResponse> extensions = transfers.stream()
				.filter(t -> t.getType() == TransferType.EXTENSION)
				.filter(t -> (t.getToTeam() != null && t.getToTeam().getId().equals(team.getId()))
						|| (t.getFromTeam() != null && t.getFromTeam().getId().equals(team.getId())))
				.map(this::toTransferResponse)
				.toList();

		List<SquadPlayerResponse> squad = playerRepository
				.findByTeamIdOrderByNameAsc(team.getId())
				.stream()
				.map(this::toSquadPlayerResponse)
				.toList();
		int contractEndWatchYear = contractEndWatchYear(competition.getSeason());
		List<SquadPlayerResponse> contractEndsNextYear = squad.stream()
				.filter(p -> p.contractEndDate() != null
						&& p.contractEndDate().getYear() == contractEndWatchYear)
				.toList();

		return new ClubMercatoResponse(
				toTeamResponse(team),
				competition.getCode(),
				competition.getName(),
				arrivals,
				departures,
				extensions,
				contractEndWatchYear,
				contractEndsNextYear,
				squad);
	}

	public PlayerDetailResponse getPlayer(Long playerId) {
		Player player = playerRepository.findByIdWithTeam(playerId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Player not found: " + playerId));
		List<TransferResponse> transfers = transferRepository
				.findByPlayerIdOrderByTransferDateDesc(playerId)
				.stream()
				.map(this::toTransferResponse)
				.toList();
		List<PlayerAppearanceResponse> appearances = matchAppearanceRepository
				.findByPlayerIdOrderByKickoffDesc(playerId)
				.stream()
				.map(a -> toAppearanceResponse(a, player.getTeam().getId()))
				.toList();
		return new PlayerDetailResponse(
				player.getId(),
				player.getName(),
				toTeamResponse(player.getTeam()),
				player.getTeam().getCompetition().getCode(),
				player.getTeam().getCompetition().getName(),
				player.getPosition(),
				player.getAge(),
				player.getHeightCm(),
				player.getWeightKg(),
				player.getNationality(),
				toTotals(appearances),
				appearances,
				transfers);
	}

	public List<StandingRowResponse> standings(String competitionCode) {
		Competition competition = getCompetition(competitionCode);
		int defensiveBonusLimit = competition.getDefensiveBonusLimit();
		OffensiveBonusRule offensiveBonusRule = competition.getOffensiveBonusRule();
		int offensiveBonusThreshold = competition.getOffensiveBonusThreshold();
		LocalDateTime seasonStart = competition.getSeasonStart().atStartOfDay();
		List<Team> teams = teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode);
		List<RugbyMatch> finished = rugbyMatchRepository.findByCompetitionCodeAndStatus(
				competitionCode, MatchStatus.FINISHED).stream()
				.filter(m -> !m.getKickoffAt().isBefore(seasonStart))
				.toList();

		Map<Long, MutableStanding> byTeamId = new HashMap<>();
		for (Team team : teams) {
			byTeamId.put(team.getId(), new MutableStanding(team));
		}

		for (RugbyMatch match : finished) {
			MutableStanding home = byTeamId.get(match.getHomeTeam().getId());
			MutableStanding away = byTeamId.get(match.getAwayTeam().getId());
			int homeScore = match.getHomeScore();
			int awayScore = match.getAwayScore();

			home.played++;
			away.played++;
			home.pointsFor += homeScore;
			home.pointsAgainst += awayScore;
			away.pointsFor += awayScore;
			away.pointsAgainst += homeScore;

			if (homeScore > awayScore) {
				home.won++;
				away.lost++;
				home.points += 4;
				if (homeScore - awayScore <= defensiveBonusLimit) {
					away.bonus++;
					away.points += 1;
				}
			} else if (awayScore > homeScore) {
				away.won++;
				home.lost++;
				away.points += 4;
				if (awayScore - homeScore <= defensiveBonusLimit) {
					home.bonus++;
					home.points += 1;
				}
			} else {
				home.drawn++;
				away.drawn++;
				home.points += 2;
				away.points += 2;
			}

			if (OffensiveBonus.earned(
					offensiveBonusRule,
					offensiveBonusThreshold,
					match.getHomeTries(),
					match.getAwayTries())) {
				home.bonus++;
				home.points += 1;
			}
			if (OffensiveBonus.earned(
					offensiveBonusRule,
					offensiveBonusThreshold,
					match.getAwayTries(),
					match.getHomeTries())) {
				away.bonus++;
				away.points += 1;
			}
		}

		List<MutableStanding> rows = new ArrayList<>(byTeamId.values());
		rows.sort(Comparator
				.comparingInt((MutableStanding s) -> s.points).reversed()
				.thenComparingInt(s -> s.pointsFor - s.pointsAgainst).reversed()
				.thenComparingInt((MutableStanding s) -> s.pointsFor).reversed()
				.thenComparing(s -> s.team.getName()));

		List<StandingRowResponse> result = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			MutableStanding s = rows.get(i);
			result.add(new StandingRowResponse(
					i + 1,
					s.team.getId(),
					s.team.getName(),
					s.team.getShortName(),
					s.played,
					s.won,
					s.drawn,
					s.lost,
					s.pointsFor,
					s.pointsAgainst,
					s.pointsFor - s.pointsAgainst,
					s.bonus,
					s.points));
		}
		return result;
	}

	private TeamFormResponse buildForm(
			List<RugbyMatch> finished,
			Long teamId,
			LocalDateTime before,
			LocalDateTime seasonStart) {
		List<RugbyMatch> currentSeason = finished.stream()
				.filter(m -> m.getKickoffAt().isBefore(before))
				.filter(m -> !m.getKickoffAt().isBefore(seasonStart))
				.filter(m -> involvesTeam(m, teamId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.limit(FORM_SIZE)
				.toList();

		List<RugbyMatch> previousSeason = List.of();
		if (currentSeason.size() < FORM_SIZE) {
			int needed = FORM_SIZE - currentSeason.size();
			previousSeason = finished.stream()
					.filter(m -> m.getKickoffAt().isBefore(before))
					.filter(m -> m.getKickoffAt().isBefore(seasonStart))
					.filter(m -> involvesTeam(m, teamId))
					.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
					.limit(needed)
					.toList();
		}

		List<RugbyMatch> teamMatches = new ArrayList<>(currentSeason);
		teamMatches.addAll(previousSeason);

		List<String> results = new ArrayList<>();
		int won = 0;
		int drawn = 0;
		int lost = 0;
		for (RugbyMatch m : teamMatches) {
			String result = resultForTeam(m, teamId);
			results.add(result);
			if ("V".equals(result)) {
				won++;
			} else if ("N".equals(result)) {
				drawn++;
			} else {
				lost++;
			}
		}
		return new TeamFormResponse(
				results,
				teamMatches.size(),
				won,
				drawn,
				lost,
				previousSeason.size());
	}

	private VenueRecordResponse buildVenueRecord(
			List<RugbyMatch> finished,
			Long teamId,
			boolean atHome,
			LocalDateTime before) {
		List<RugbyMatch> venueMatches = finished.stream()
				.filter(m -> m.getKickoffAt().isBefore(before))
				.filter(m -> atHome
						? m.getHomeTeam().getId().equals(teamId)
						: m.getAwayTeam().getId().equals(teamId))
				.toList();

		int won = 0;
		int drawn = 0;
		int lost = 0;
		for (RugbyMatch m : venueMatches) {
			String result = resultForTeam(m, teamId);
			if ("V".equals(result)) {
				won++;
			} else if ("N".equals(result)) {
				drawn++;
			} else {
				lost++;
			}
		}
		return new VenueRecordResponse(venueMatches.size(), won, drawn, lost);
	}

	private List<HeadToHeadMatchResponse> buildHeadToHead(
			List<RugbyMatch> finished,
			Long homeId,
			Long awayId,
			LocalDateTime before) {
		return finished.stream()
				.filter(m -> m.getKickoffAt().isBefore(before))
				.filter(m -> isHeadToHead(m, homeId, awayId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.limit(HEAD_TO_HEAD_SIZE)
				.map(m -> new HeadToHeadMatchResponse(
						m.getId(),
						m.getKickoffAt(),
						m.getHomeTeam().getShortName(),
						m.getAwayTeam().getShortName(),
						m.getHomeScore(),
						m.getAwayScore()))
				.toList();
	}

	private boolean involvesTeam(RugbyMatch match, Long teamId) {
		return match.getHomeTeam().getId().equals(teamId)
				|| match.getAwayTeam().getId().equals(teamId);
	}

	private boolean isHeadToHead(RugbyMatch match, Long teamA, Long teamB) {
		Long homeId = match.getHomeTeam().getId();
		Long awayId = match.getAwayTeam().getId();
		return (homeId.equals(teamA) && awayId.equals(teamB))
				|| (homeId.equals(teamB) && awayId.equals(teamA));
	}

	private String resultForTeam(RugbyMatch match, Long teamId) {
		boolean isHome = match.getHomeTeam().getId().equals(teamId);
		int teamScore = isHome ? match.getHomeScore() : match.getAwayScore();
		int oppScore = isHome ? match.getAwayScore() : match.getHomeScore();
		if (teamScore > oppScore) {
			return "V";
		}
		if (teamScore < oppScore) {
			return "D";
		}
		return "N";
	}

	private SquadPlayerResponse toSquadPlayerResponse(Player player) {
		return new SquadPlayerResponse(
				player.getId(),
				player.getName(),
				player.getPosition(),
				player.getAge(),
				player.getHeightCm(),
				player.getWeightKg(),
				player.getNationality(),
				player.getContractEndDate());
	}

	private static int contractEndWatchYear(String season) {
		String[] parts = season.split("-");
		if (parts.length == 2) {
			try {
				return Integer.parseInt(parts[1].trim()) + 1;
			} catch (NumberFormatException ignored) {
				// ignore
			}
		}
		return LocalDate.now().getYear() + 1;
	}

	private TransferResponse toTransferResponse(Transfer transfer) {
		return new TransferResponse(
				transfer.getId(),
				transfer.getTransferDate(),
				transfer.getPlayerName(),
				transfer.getPlayer() != null ? transfer.getPlayer().getId() : null,
				transfer.getType().name(),
				clubLabel(transfer.getFromTeam(), transfer.getFromClubName()),
				clubLabel(transfer.getToTeam(), transfer.getToClubName()),
				transfer.getFromTeam() != null ? transfer.getFromTeam().getId() : null,
				transfer.getToTeam() != null ? transfer.getToTeam().getId() : null,
				transfer.getContractLength(),
				transfer.getCompetition().getCode(),
				transfer.getCompetition().getName());
	}

	private PlayerAppearanceResponse toAppearanceResponse(MatchAppearance appearance, Long playerTeamId) {
		RugbyMatch match = appearance.getMatch();
		boolean home = match.getHomeTeam().getId().equals(playerTeamId);
		Team opponent = home ? match.getAwayTeam() : match.getHomeTeam();
		Integer teamScore = home ? match.getHomeScore() : match.getAwayScore();
		Integer oppScore = home ? match.getAwayScore() : match.getHomeScore();
		return new PlayerAppearanceResponse(
				match.getId(),
				match.getKickoffAt(),
				match.getMatchday(),
				match.getCompetition().getCode(),
				opponent.getShortName(),
				home ? "DOM" : "EXT",
				resultLabel(teamScore, oppScore),
				match.getHomeScore(),
				match.getAwayScore(),
				appearance.getJerseyNumber(),
				appearance.isStarter(),
				appearance.getMinutesPlayed(),
				appearance.getTries(),
				appearance.getYellowCards(),
				appearance.getRedCards());
	}

	private static PlayerTotalsResponse toTotals(List<PlayerAppearanceResponse> appearances) {
		int starts = 0;
		int minutes = 0;
		int tries = 0;
		int yellow = 0;
		int red = 0;
		for (PlayerAppearanceResponse a : appearances) {
			if (a.starter()) {
				starts++;
			}
			minutes += a.minutesPlayed();
			tries += a.tries();
			yellow += a.yellowCards();
			red += a.redCards();
		}
		return new PlayerTotalsResponse(appearances.size(), starts, minutes, tries, yellow, red);
	}

	private static String resultLabel(Integer teamScore, Integer oppScore) {
		if (teamScore == null || oppScore == null) {
			return "—";
		}
		if (teamScore > oppScore) {
			return "V";
		}
		if (teamScore < oppScore) {
			return "D";
		}
		return "N";
	}

	private String clubLabel(Team team, String fallbackName) {
		if (team != null) {
			return team.getShortName();
		}
		if (fallbackName != null && !fallbackName.isBlank()) {
			return fallbackName;
		}
		return "—";
	}

	private void ensureCompetitionExists(String competitionCode) {
		getCompetition(competitionCode);
	}

	private Competition getCompetition(String competitionCode) {
		return competitionRepository.findByCode(competitionCode)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Competition not found: " + competitionCode));
	}

	private TeamResponse toTeamResponse(Team team) {
		return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getCity());
	}

	private MatchResponse toMatchResponse(
			RugbyMatch match,
			List<AbsenceResponse> homeAbsences,
			List<AbsenceResponse> awayAbsences,
			TeamFormResponse homeForm,
			TeamFormResponse awayForm,
			VenueRecordResponse homeHomeRecord,
			VenueRecordResponse awayAwayRecord,
			List<HeadToHeadMatchResponse> headToHead) {
		return new MatchResponse(
				match.getId(),
				match.getMatchday(),
				match.getKickoffAt(),
				match.getStatus().name(),
				toTeamResponse(match.getHomeTeam()),
				toTeamResponse(match.getAwayTeam()),
				match.getHomeScore(),
				match.getAwayScore(),
				match.getAnalysis(),
				homeAbsences,
				awayAbsences,
				homeForm,
				awayForm,
				homeHomeRecord,
				awayAwayRecord,
				headToHead);
	}

	private List<AbsenceResponse> toAbsenceResponses(List<Absence> absences) {
		return absences.stream()
				.map(absence -> new AbsenceResponse(
						absence.getPlayer().getName(),
						absence.getType().name(),
						absence.getNote()))
				.toList();
	}

	private static final class MutableStanding {
		private final Team team;
		private int played;
		private int won;
		private int drawn;
		private int lost;
		private int pointsFor;
		private int pointsAgainst;
		private int bonus;
		private int points;

		private MutableStanding(Team team) {
			this.team = team;
		}
	}
}
