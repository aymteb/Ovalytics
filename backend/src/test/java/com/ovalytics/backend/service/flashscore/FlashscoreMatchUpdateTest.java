package com.ovalytics.backend.service.flashscore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ovalytics.backend.domain.MatchStatus;

class FlashscoreMatchUpdateTest {

	@Test
	void mapsLiveAndFinishedStatuses() {
		Map<String, String> liveFields = Map.of(
				"AA", "evt1",
				"AE", "RC Toulon",
				"AF", "Union Bordeaux-Begles",
				"AD", "1700000000",
				"AB", "2",
				"AG", "10",
				"AH", "7",
				"CR", "5");

		FlashscoreMatchUpdate live = FlashscoreMatchUpdate.fromFields(
				liveFields, "TOP14", "TOL", "UBB");

		assertThat(live.status()).isEqualTo(MatchStatus.LIVE);
		assertThat(live.homeScore()).isEqualTo(10);
		assertThat(live.awayScore()).isEqualTo(7);

		Map<String, String> finishedFields = Map.of(
				"AA", "evt2",
				"AE", "RC Toulon",
				"AF", "Union Bordeaux-Begles",
				"AD", "1700000000",
				"AB", "3",
				"AG", "24",
				"AH", "19");

		FlashscoreMatchUpdate finished = FlashscoreMatchUpdate.fromFields(
				finishedFields, "TOP14", "TOL", "UBB");

		assertThat(finished.status()).isEqualTo(MatchStatus.FINISHED);
		assertThat(finished.homeScore()).isEqualTo(24);
		assertThat(finished.awayScore()).isEqualTo(19);
	}

	@Test
	void parsesFeedChunks() {
		String raw = "AA÷abc¬AE÷Toulon¬AF÷Bordeaux Begles¬AD÷1700000000¬AB÷2¬AG÷3¬AH÷0";
		List<Map<String, String>> events = FlashscoreEventParser.parseFeed(raw);
		assertThat(events).hasSize(1);
		assertThat(events.get(0).get("AA")).isEqualTo("abc");
		assertThat(events.get(0).get("AG")).isEqualTo("3");
	}
}
