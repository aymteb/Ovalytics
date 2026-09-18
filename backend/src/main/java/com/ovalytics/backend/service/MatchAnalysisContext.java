package com.ovalytics.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ovalytics.backend.domain.MatchStatus;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.web.dto.StandingRowResponse;

record MatchAnalysisContext(
		StandingRowResponse homeStanding,
		StandingRowResponse awayStanding,
		List<NarrowLoss> homeNarrowLosses,
		ScoredOuting visitorLastAway,
		int visitorAwayGamesThisSeason,
		int tableSize) {

	record NarrowLoss(int margin) {
	}

	record ScoredOuting(
			String opponentName,
			String opponentShortName,
			int teamScore,
			int opponentScore,
			boolean heavyDefeat) {
	}

	int homeNarrowLossCount() {
		return homeNarrowLosses.size();
	}

	int homeNarrowLossMarginSum() {
		return homeNarrowLosses.stream().mapToInt(NarrowLoss::margin).sum();
	}

	static MatchAnalysisContext from(
			RugbyMatch match,
			List<StandingRowResponse> table,
			List<RugbyMatch> seasonFinishedBefore) {
		Long homeId = match.getHomeTeam().getId();
		Long awayId = match.getAwayTeam().getId();
		StandingRowResponse homeStanding = findRow(table, homeId);
		StandingRowResponse awayStanding = findRow(table, awayId);

		List<NarrowLoss> narrowLosses = new ArrayList<>();
		List<RugbyMatch> homeSeason = seasonFinishedBefore.stream()
				.filter(m -> involves(m, homeId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.toList();
		for (RugbyMatch m : homeSeason) {
			Integer margin = defeatMargin(m, homeId);
			if (margin != null && margin > 0 && margin <= 8) {
				narrowLosses.add(new NarrowLoss(margin));
			} else if (margin != null && margin > 8) {
				break;
			} else if (margin == null && won(m, homeId)) {
				break;
			}
		}

		List<RugbyMatch> visitorAwayGames = seasonFinishedBefore.stream()
				.filter(m -> m.getAwayTeam().getId().equals(awayId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.toList();
		ScoredOuting lastAway = visitorAwayGames.stream()
				.findFirst()
				.map(m -> {
					int forScore = m.getAwayScore();
					int against = m.getHomeScore();
					return new ScoredOuting(
							m.getHomeTeam().getName(),
							m.getHomeTeam().getShortName(),
							forScore,
							against,
							against - forScore >= 15);
				})
				.orElse(null);

		return new MatchAnalysisContext(
				homeStanding,
				awayStanding,
				List.copyOf(narrowLosses),
				lastAway,
				visitorAwayGames.size(),
				table.size());
	}

	static List<RugbyMatch> seasonFinishedBefore(
			List<RugbyMatch> finished,
			LocalDateTime seasonStart,
			LocalDateTime before) {
		return finished.stream()
				.filter(m -> m.getStatus() == MatchStatus.FINISHED)
				.filter(m -> !m.getKickoffAt().isBefore(seasonStart))
				.filter(m -> m.getKickoffAt().isBefore(before))
				.toList();
	}

	private static StandingRowResponse findRow(List<StandingRowResponse> table, Long teamId) {
		return table.stream()
				.filter(row -> row.teamId().equals(teamId))
				.findFirst()
				.orElse(null);
	}

	private static boolean involves(RugbyMatch match, Long teamId) {
		return match.getHomeTeam().getId().equals(teamId)
				|| match.getAwayTeam().getId().equals(teamId);
	}

	private static boolean won(RugbyMatch match, Long teamId) {
		boolean home = match.getHomeTeam().getId().equals(teamId);
		int forScore = home ? match.getHomeScore() : match.getAwayScore();
		int against = home ? match.getAwayScore() : match.getHomeScore();
		return forScore > against;
	}

	private static Integer defeatMargin(RugbyMatch match, Long teamId) {
		boolean home = match.getHomeTeam().getId().equals(teamId);
		int forScore = home ? match.getHomeScore() : match.getAwayScore();
		int against = home ? match.getAwayScore() : match.getHomeScore();
		if (forScore >= against) {
			return null;
		}
		return against - forScore;
	}
}
