package com.ovalytics.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ovalytics.backend.domain.OffensiveBonusRule;

class OffensiveBonusTest {

	@Test
	void tryDifferenceNeedsThreeMoreTries() {
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRY_DIFFERENCE, 3, 5, 2)).isTrue();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRY_DIFFERENCE, 3, 4, 2)).isFalse();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRY_DIFFERENCE, 3, 3, 0)).isTrue();
	}

	@Test
	void triesScoredNeedsAbsoluteCount() {
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRIES_SCORED, 4, 4, 1)).isTrue();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRIES_SCORED, 4, 3, 0)).isFalse();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRIES_SCORED, 4, 5, 5)).isTrue();
	}

	@Test
	void missingTriesMeansNoBonus() {
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRY_DIFFERENCE, 3, null, 2)).isFalse();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRY_DIFFERENCE, 3, 5, null)).isFalse();
		assertThat(OffensiveBonus.earned(OffensiveBonusRule.TRIES_SCORED, 4, null, null)).isFalse();
	}
}
