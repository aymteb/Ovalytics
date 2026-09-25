package com.ovalytics.backend.service.flashscore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FlashscoreLineupParserTest {

	@Test
	void parsesHomeAwayStartersAndBench() {
		String raw = String.join(
				"¬~",
				"LB÷Starting Lineups¬LGT÷1¬LC÷1",
				"LH÷0¬LI÷Baille C.¬LJ÷1¬LK÷1¬LG÷1",
				"LH÷1¬LI÷Dupont A.¬LJ÷9¬LK÷1¬LG÷1",
				"LC÷2",
				"LH÷0¬LI÷Perchaud M.¬LJ÷1¬LK÷1¬LG÷1",
				"LB÷Substitutes¬LGT÷1¬LC÷1",
				"LH÷15¬LI÷Mauvaka P.¬LJ÷16¬LK÷2¬LG÷1",
				"LC÷2",
				"LH÷15¬LI÷Barlot G.¬LJ÷16¬LK÷2¬LG÷1");

		List<FlashscoreLineupParser.LineupPlayer> rows = FlashscoreLineupParser.parse(raw);

		assertThat(rows).hasSize(5);
		assertThat(rows.get(0).teamSide()).isEqualTo("HOME");
		assertThat(rows.get(0).playerName()).isEqualTo("Baille C.");
		assertThat(rows.get(0).starter()).isTrue();
		assertThat(rows.get(2).teamSide()).isEqualTo("AWAY");
		assertThat(rows.get(2).playerName()).isEqualTo("Perchaud M.");
		assertThat(rows.get(3).starter()).isFalse();
		assertThat(rows.get(3).jerseyNumber()).isEqualTo(16);
		assertThat(rows.get(4).teamSide()).isEqualTo("AWAY");
	}
}
