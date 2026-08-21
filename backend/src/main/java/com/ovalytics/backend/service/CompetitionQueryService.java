package com.ovalytics.backend.service;

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
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamResponse;

@Service
public class CompetitionQueryService {

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
				.map(match -> toMatchResponse(match, List.of(), List.of()))
				.toList();
	}

	public MatchResponse getMatch(String competitionCode, Long matchId) {
		ensureCompetitionExists(competitionCode);
		RugbyMatch match = rugbyMatchRepository
				.findByCompetitionCodeAndId(competitionCode, matchId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Match not found: " + matchId));
		return toMatchResponse(
				match,
				toAbsenceResponses(absenceRepository.findByTeamId(match.getHomeTeam().getId())),
				toAbsenceResponses(absenceRepository.findByTeamId(match.getAwayTeam().getId())));
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
			List<AbsenceResponse> awayAbsences) {
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
				awayAbsences);
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
