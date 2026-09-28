package com.ovalytics.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ovalytics.backend.domain.MatchEvent;
import com.ovalytics.backend.domain.MatchEventType;
import com.ovalytics.backend.domain.RugbyMatch;
import com.ovalytics.backend.repository.MatchEventRepository;
import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.HeadToHeadMatchResponse;
import com.ovalytics.backend.web.dto.MatchLineupResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;
import com.ovalytics.backend.web.dto.VenueRecordResponse;

@Component
public class MatchAnalysisFactsBuilder {

	private final MatchEventRepository matchEventRepository;

	public MatchAnalysisFactsBuilder(MatchEventRepository matchEventRepository) {
		this.matchEventRepository = matchEventRepository;
	}

	public String build(
			MatchResponse match,
			MatchAnalysisContext context,
			List<RugbyMatch> seasonFinishedBefore) {
		StringBuilder sb = new StringBuilder();
		sb.append("COMPETITION: ").append(match.competitionName()).append(" (").append(match.competitionCode()).append(")\n");
		sb.append("JOURNEE: ").append(match.matchday()).append("\n");
		sb.append("COUP D'ENVOI: ").append(match.kickoffAt()).append("\n");
		sb.append("AFFICHE: ")
				.append(match.homeTeam().name()).append(" (").append(match.homeTeam().shortName()).append(")")
				.append(" vs ")
				.append(match.awayTeam().name()).append(" (").append(match.awayTeam().shortName()).append(")")
				.append("\n\n");

		appendStanding(sb, "DOMICILE", match.homeTeam().shortName(), context.homeStanding(), context.tableSize());
		appendStanding(sb, "EXTERIEUR", match.awayTeam().shortName(), context.awayStanding(), context.tableSize());
		sb.append('\n');

		appendForm(sb, "FORME DOMICILE (tous matchs)", match.homeForm());
		appendForm(sb, "FORME EXTERIEUR (tous matchs)", match.awayForm());
		appendVenue(sb, "BILAN DOMICILE a domicile", match.homeHomeRecord());
		appendVenue(sb, "BILAN EXTERIEUR a l'exterieur", match.awayAwayRecord());
		sb.append('\n');

		appendAbsences(sb, "ABSENTS DOMICILE", match.homeAbsences());
		appendAbsences(sb, "ABSENTS EXTERIEUR", match.awayAbsences());
		sb.append('\n');

		appendLineups(sb, match);
		sb.append('\n');

		appendHeadToHead(sb, match.headToHead());
		sb.append('\n');

		if (context.homeNarrowLossCount() > 0) {
			sb.append("SIGNALS DOMICILE: ")
					.append(context.homeNarrowLossCount())
					.append(" defaites recentes a 8 points ou moins (marges cumulees ")
					.append(context.homeNarrowLossMarginSum())
					.append(").\n");
		}
		if (context.visitorLastAway() != null) {
			MatchAnalysisContext.ScoredOuting last = context.visitorLastAway();
			sb.append("DERNIER DEPLACEMENT VISITEUR: ")
					.append(last.teamScore()).append("-").append(last.opponentScore())
					.append(" chez ").append(last.opponentShortName());
			if (last.heavyDefeat()) {
				sb.append(" (lourde defaite)");
			}
			sb.append(".\n");
		}
		sb.append('\n');

		appendRecentSheets(
				sb,
				"FEUILLE RECENTE DOMICILE",
				match.homeTeam().id(),
				match.homeTeam().shortName(),
				seasonFinishedBefore);
		appendRecentSheets(
				sb,
				"FEUILLE RECENTE EXTERIEUR",
				match.awayTeam().id(),
				match.awayTeam().shortName(),
				seasonFinishedBefore);

		return sb.toString();
	}

	private void appendRecentSheets(
			StringBuilder sb,
			String title,
			Long teamId,
			String shortName,
			List<RugbyMatch> seasonFinishedBefore) {
		if (teamId == null) {
			return;
		}
		List<RugbyMatch> recent = seasonFinishedBefore.stream()
				.filter(m -> involves(m, teamId))
				.sorted(Comparator.comparing(RugbyMatch::getKickoffAt).reversed())
				.limit(3)
				.toList();
		if (recent.isEmpty()) {
			sb.append(title).append(": aucune feuille recente.\n");
			return;
		}
		sb.append(title).append(" (3 derniers matchs):\n");
		for (RugbyMatch finished : recent) {
			boolean home = finished.getHomeTeam().getId().equals(teamId);
			String side = home ? "HOME" : "AWAY";
			List<MatchEvent> events = matchEventRepository.findByMatchIdOrderBySortOrderAsc(finished.getId());
			int tries = 0;
			int yellow = 0;
			int red = 0;
			List<String> cardNames = new ArrayList<>();
			for (MatchEvent event : events) {
				if (!side.equals(event.getTeamSide())) {
					continue;
				}
				if (event.getEventType() == MatchEventType.TRY) {
					tries++;
				} else if (event.getEventType() == MatchEventType.YELLOW) {
					yellow++;
					cardNames.add("jaune " + event.getPlayerName() + " " + event.getMinuteLabel());
				} else if (event.getEventType() == MatchEventType.RED) {
					red++;
					cardNames.add("rouge " + event.getPlayerName() + " " + event.getMinuteLabel());
				}
			}
			sb.append("- vs ")
					.append(home ? finished.getAwayTeam().getShortName() : finished.getHomeTeam().getShortName())
					.append(" ")
					.append(finished.getHomeScore()).append("-").append(finished.getAwayScore())
					.append(" | essais equipe ").append(shortName).append("=").append(tries)
					.append(" | cartons J/R=").append(yellow).append("/").append(red);
			if (!cardNames.isEmpty()) {
				sb.append(" (").append(String.join(", ", cardNames)).append(")");
			}
			sb.append('\n');
		}
	}

	private static boolean involves(RugbyMatch match, Long teamId) {
		return match.getHomeTeam().getId().equals(teamId)
				|| match.getAwayTeam().getId().equals(teamId);
	}

	private static void appendStanding(
			StringBuilder sb,
			String label,
			String shortName,
			StandingRowResponse row,
			int tableSize) {
		sb.append("CLASSEMENT ").append(label).append(" (").append(shortName).append("): ");
		if (row == null) {
			sb.append("indisponible.\n");
			return;
		}
		sb.append(row.position()).append("/").append(tableSize)
				.append(" | ").append(row.points()).append(" pts")
				.append(" | ").append(row.played()).append(" joues")
				.append(" | ").append(row.won()).append("V/")
				.append(row.drawn()).append("N/")
				.append(row.lost()).append("D")
				.append('\n');
	}

	private static void appendForm(StringBuilder sb, String label, TeamFormResponse form) {
		sb.append(label).append(": ");
		if (form == null || form.played() == 0) {
			sb.append("insuffisante.\n");
			return;
		}
		sb.append(String.join("-", form.results()))
				.append(" (").append(form.won()).append("V/")
				.append(form.drawn()).append("N/")
				.append(form.lost()).append("D");
		if (form.fromPreviousSeason() > 0) {
			sb.append(", dont ").append(form.fromPreviousSeason()).append(" saison derniere");
		}
		sb.append(").\n");
	}

	private static void appendVenue(StringBuilder sb, String label, VenueRecordResponse record) {
		sb.append(label).append(": ");
		if (record == null || record.played() == 0) {
			sb.append("insuffisant.\n");
			return;
		}
		sb.append(record.won()).append("V/")
				.append(record.drawn()).append("N/")
				.append(record.lost()).append("D sur ")
				.append(record.played()).append(" matchs.\n");
	}

	private static void appendAbsences(StringBuilder sb, String label, List<AbsenceResponse> absences) {
		sb.append(label).append(": ");
		if (absences == null || absences.isEmpty()) {
			sb.append("aucun signale.\n");
			return;
		}
		List<String> parts = new ArrayList<>();
		for (AbsenceResponse absence : absences) {
			parts.add(absence.playerName() + " (" + absence.type().toLowerCase(Locale.ROOT) + ")");
		}
		sb.append(String.join(", ", parts)).append(".\n");
	}

	private static void appendLineups(StringBuilder sb, MatchResponse match) {
		List<MatchLineupResponse> lineups = match.lineups();
		if (lineups == null || lineups.isEmpty()) {
			sb.append("COMPOS: non publiees encore (ne pas inventer de XV).\n");
			return;
		}
		appendSideLineup(sb, "COMPOS DOMICILE", "HOME", lineups);
		appendSideLineup(sb, "COMPOS EXTERIEUR", "AWAY", lineups);
	}

	private static void appendSideLineup(
			StringBuilder sb,
			String label,
			String side,
			List<MatchLineupResponse> lineups) {
		List<MatchLineupResponse> starters = lineups.stream()
				.filter(row -> side.equals(row.teamSide()) && row.starter())
				.sorted(Comparator.comparingInt(MatchLineupResponse::jerseyNumber))
				.toList();
		sb.append(label).append(": ");
		if (starters.isEmpty()) {
			sb.append("non publiee.\n");
			return;
		}
		List<String> parts = new ArrayList<>();
		for (MatchLineupResponse row : starters) {
			parts.add(row.jerseyNumber() + " " + row.playerName());
		}
		sb.append(String.join(", ", parts)).append(".\n");
	}

	private static void appendHeadToHead(StringBuilder sb, List<HeadToHeadMatchResponse> h2h) {
		sb.append("CONFRONTATIONS DIRECTES RECENTES:\n");
		if (h2h == null || h2h.isEmpty()) {
			sb.append("- aucune.\n");
			return;
		}
		h2h.stream().limit(5).forEach(row -> sb.append("- ")
				.append(row.homeShortName()).append(" ")
				.append(row.homeScore()).append("-").append(row.awayScore())
				.append(" ").append(row.awayShortName())
				.append(" (").append(row.kickoffAt().toLocalDate()).append(")\n"));
	}
}
