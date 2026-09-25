package com.ovalytics.backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchResponse(
		Long id,
		String competitionCode,
		String competitionName,
		int matchday,
		LocalDateTime kickoffAt,
		String status,
		TeamResponse homeTeam,
		TeamResponse awayTeam,
		Integer homeScore,
		Integer awayScore,
		Integer homeTries,
		Integer awayTries,
		String analysis,
		List<AbsenceResponse> homeAbsences,
		List<AbsenceResponse> awayAbsences,
		TeamFormResponse homeForm,
		TeamFormResponse awayForm,
		VenueRecordResponse homeHomeRecord,
		VenueRecordResponse awayAwayRecord,
		List<HeadToHeadMatchResponse> headToHead,
		List<MatchEventResponse> events,
		List<MatchLineupResponse> lineups) {
}
