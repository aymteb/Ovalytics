package com.ovalytics.backend.service.rugbyrama;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RugbyramaNationaleLineupClientTest {

	@Test
	void parsesStartersAndBench() {
		String raw = """
				Massy : 15. Joly ; 14. Gogoladze, 13.Cusson, 12. Mignot, 11. Artaud ; 10. Hutley ; 9. Rouet ; 7. Louiere (cap.), 8. Nieto, 6. Konate ; 5. Djebbari, 4.Riou ; 3. Visser 2. Trassoudaine, 1.Fisi’ihoi.

				Remplaçants : 16. Poisson, 17. Poipy, 18. Mahu, 19. Marchesin, 20. Rubio, 21. Vidalenc, 22. Carré, 23. Ferrer.
				Mont-de-Marsan : 15. De Nardi ; 14.Leraitre, 13. Tuimaba, 12. Dachary, 11. Robbe ; 10. Barreau, 9. Milo-Harris ; 7. Ponpon (cap.), 8. Godfrey, 6.Reynaud ; 5. Liufau, 4. Kirsten ; 3.Tafili, 2. Van Jaarsveld, 1. Avaloy.

				Remplaçants : 16. Dufour, 17. Lewille, 18. Dussutour, 19. Lafforgue, 20. Maka, 21. Du Plessis, 22. Manu, 23.Tapueluelu.
				""";

		Map<String, RugbyramaNationaleLineupClient.TeamBlock> blocks =
				RugbyramaNationaleLineupClient.parseArticle(raw);

		assertThat(blocks).containsKeys("mas", "mdm");
		assertThat(blocks.get("mas").players()).hasSizeGreaterThanOrEqualTo(20);
		assertThat(blocks.get("mas").players().get(0).jersey()).isEqualTo(15);
		assertThat(blocks.get("mas").players().get(0).name()).isEqualTo("Joly");
		assertThat(blocks.get("mas").players().stream().anyMatch(p -> p.captain() && p.name().contains("Louiere")))
				.isTrue();
		assertThat(blocks.get("mdm").players().stream().anyMatch(p -> p.jersey() == 16 && !p.starter()))
				.isTrue();
	}

	@Test
	void keepsFirstNameWhenAlternative() {
		List<RugbyramaNationaleLineupClient.ParsedPlayer> players =
				RugbyramaNationaleLineupClient.parsePlayers(
						"15. Descamps ou Vaovasa ; 14. Rasaku",
						true);
		assertThat(players.get(0).name()).isEqualTo("Descamps");
	}

	@Test
	void acceptsCommaAsJerseySeparator() {
		List<RugbyramaNationaleLineupClient.ParsedPlayer> players =
				RugbyramaNationaleLineupClient.parsePlayers(
						"18. Malataweru, 19, Vidal, 20. Amosa",
						false);
		assertThat(players).extracting(RugbyramaNationaleLineupClient.ParsedPlayer::jersey)
				.containsExactly(18, 19, 20);
		assertThat(players.get(1).name()).isEqualTo("Vidal");
	}
}
