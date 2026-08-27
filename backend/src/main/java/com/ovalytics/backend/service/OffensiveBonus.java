package com.ovalytics.backend.service;

import com.ovalytics.backend.domain.OffensiveBonusRule;

final class OffensiveBonus {

	private OffensiveBonus() {
	}

	static boolean earned(
			OffensiveBonusRule rule,
			int threshold,
			Integer teamTries,
			Integer opponentTries) {
		if (teamTries == null || rule == null) {
			return false;
		}
		return switch (rule) {
			case TRY_DIFFERENCE -> opponentTries != null && teamTries - opponentTries >= threshold;
			case TRIES_SCORED -> teamTries >= threshold;
		};
	}
}
