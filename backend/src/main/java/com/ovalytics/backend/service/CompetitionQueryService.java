package com.ovalytics.backend.service;

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
import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.AbsenceRepository;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;
import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.HeadToHeadMatchResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;
import com.ovalytics.backend.web.dto.TeamResponse;
import com.ovalytics.backend.web.dto.VenueRecordResponse;

@Service
public class CompetitionQueryService {

	private static final int FORM_SIZE = 5;
	private static final int HEAD_TO_HEAD_SIZE = 5;

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;
	private final AbsenceRepository absenceRepository;

	public CompetitionQueryService(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository,
			AbsenceRepository absenceRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
		this.absenceRepository = absenceRepository;
	}

	public List<TeamResponse> listTeams(String competitionCode) {
		ensureCompetitionExists(competitionCode);
		return teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode).stream()
				.map(this::toTeamResponse)
				.toList();
	}

	public List<MatchResponse> listMatches(String competitionCode, MatchStatus status) {
		ensureCompetitionExists(competitionCode);
		List<RugbyMatch> matches = status == null
				? rugbyMatchRepository.findByCompetitionCode(competitionCode)
				: rugbyMatchRepository.findByCompetitionCodeAndStatus(competitionCode, status);
		return matches.stream()
				.map(match -> toMatchResponse(match, List.of(), List.of(), null, null, null, null, List.of()))
				.toList();
	}

	public MatchResponse getMatch(String competitionCode, Long matchId) {
		ensureCompetitionExists(competitionCode);
		RugbyMatch match = rugbyMatchRepository
				.findByCompetitionCodeAndId(competitionCode, matchId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Match not found: " + matchId));

		List<RugbyMatch> finished = rugbyMatchRepository
				.findByCompetitionCodeAndStatus(competitionCode, MatchStatus.FINISHED);
		LocalDateTime before = match.getKickoffAt();
		Long homeId = match.getHomeTeam().getId();
		Long awayId = match.getAwayTeam().getId();

		return toMatchResponse(
				match,
				toAbsenceResponses(absenceRepository.findByTeamId(homeId)),
				toAbsenceResponses(absenceRepository.findByTeamId(awayId)),
				buildForm(finished, homeId, before),
				buildForm(finished, awayId, before),
				buildVenueRecord(finished, homeId, true, before),
				buildVenueRecord(finished, awayId, false, before),
				buildHeadToHead(finished, homeId, awayId, before));
	}

	public List<StandingRowResponse> standings(String competitionCode) {
		Competition competition = getCompetition(competitionCode);
		int defensiveBonusLimit = competition.getDefensiveBonusLimit();
		List<Team> teams = teamRepository.findByCompetitionCodeOrderByNameAsc(competitionCode);
		List<RugbyMatch> finished = rugbyMatchRepository.findByCompetitionCodeAndStatus(
				competitionCode, MatchStatus.FINISHED);

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

	private TeamFormResponse buildForm(List<RugbyMatch> finished, Long teamId, LocalDateTime before) {
		List<RugbyMatch> teamMatches = finished.stream()
				.filter(m -> m.getKickoffAt().isBefore(before))
				.filter(m -> involvesTeam(m, teamId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.limit(FORM_SIZE)
				.toList();

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
		return new TeamFormResponse(results, teamMatches.size(), won, drawn, lost);
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
