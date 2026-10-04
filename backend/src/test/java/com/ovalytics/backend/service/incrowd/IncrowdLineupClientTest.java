package com.ovalytics.backend.service.incrowd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IncrowdLineupClientTest {

	@Test
	void matchesPremiershipTeamNames() {
		assertThat(IncrowdLineupClient.sameTeam("Bath Rugby", "Bath Rugby", "BAT")).isTrue();
		assertThat(IncrowdLineupClient.sameTeam("Leicester Tigers", "Leicester Tigers", "LEIC")).isTrue();
		assertThat(IncrowdLineupClient.sameTeam("Newcastle Red Bulls", "Newcastle Falcons", "NEW"))
				.isTrue();
		assertThat(IncrowdLineupClient.sameTeam("Saracens", "Saracens", "SAR")).isTrue();
	}

	@Test
	void matchesUrcTeamNames() {
		assertThat(IncrowdLineupClient.sameTeam("Leinster Rugby", "Leinster", "LEI")).isTrue();
		assertThat(IncrowdLineupClient.sameTeam("DHL Stormers", "Stormers", "STO")).isTrue();
		assertThat(IncrowdLineupClient.sameTeam("Benetton Rugby", "Benetton Treviso", "TRE")).isTrue();
	}

	@Test
	void rejectsDifferentTeams() {
		assertThat(IncrowdLineupClient.sameTeam("Bath Rugby", "Exeter Chiefs", "EXE")).isFalse();
	}
}
