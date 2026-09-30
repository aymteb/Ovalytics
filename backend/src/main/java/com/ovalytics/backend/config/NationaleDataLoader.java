package com.ovalytics.backend.config;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Competition;
import com.ovalytics.backend.domain.OffensiveBonusRule;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.CompetitionRepository;
import com.ovalytics.backend.repository.RugbyMatchRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
@Order(3)
public class NationaleDataLoader implements ApplicationRunner {

	private static final LocalDate SEASON_START = LocalDate.of(2026, 8, 1);
	private static final String SEASON = "2026-2027";

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public NationaleDataLoader(
			CompetitionRepository competitionRepository,
			TeamRepository teamRepository,
			RugbyMatchRepository rugbyMatchRepository) {
		this.competitionRepository = competitionRepository;
		this.teamRepository = teamRepository;
		this.rugbyMatchRepository = rugbyMatchRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		var existing = competitionRepository.findByCode("NAT");
		if (existing.isPresent()) {
			Competition nationale = existing.get();
			nationale.setSeason(SEASON);
			nationale.setSeasonStart(SEASON_START);
			seedTeamsIfNeeded(nationale);
			return;
		}

		Competition nationale = competitionRepository.save(
				new Competition(
						"Nationale",
						"NAT",
						SEASON,
						SEASON_START,
						5,
						OffensiveBonusRule.TRY_DIFFERENCE,
						3));
		seedTeams(nationale);
	}

	private void seedTeams(Competition nationale) {
		for (Team team : nationaleTeams(nationale)) {
			teamRepository.save(team);
		}
	}

	private void seedTeamsIfNeeded(Competition nationale) {
		Map<String, Team> existing = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc("NAT")) {
			existing.put(team.getShortName(), team);
		}
		Set<String> keep = new HashSet<>();
		for (Team team : nationaleTeams(nationale)) {
			keep.add(team.getShortName());
			if (!existing.containsKey(team.getShortName())) {
				teamRepository.save(team);
			}
		}
		for (Team team : existing.values()) {
			if (!keep.contains(team.getShortName())
					&& rugbyMatchRepository.countByTeamId(team.getId()) == 0) {
				teamRepository.delete(team);
			}
		}
	}

	private static List<Team> nationaleTeams(Competition nationale) {
		return List.of(
				new Team("Rugby Club Massy Essonne", "MAS", "Massy", nationale),
				new Team("US Carcassonne", "CAR", "Carcassonne", nationale),
				new Team("Sporting Club Albigeois", "ALB", "Albi", nationale),
				new Team("Stade Montois", "MDM", "Mont-de-Marsan", nationale),
				new Team("SO Chambéry", "CHA", "Chambéry", nationale),
				new Team("Rouen Normandie Rugby", "ROU", "Rouen", nationale),
				new Team("RC Suresnes", "SUR", "Suresnes", nationale),
				new Team("CS Bourgoin-Jallieu", "BOU", "Bourgoin", nationale),
				new Team("RC Orléans", "ORL", "Orléans", nationale),
				new Team("CA Périgueux", "PER", "Périgueux", nationale),
				new Team("Rennes Étudiants Club", "REN", "Rennes", nationale),
				new Team("CS Vienne", "VIE", "Vienne", nationale),
				new Team("US Bressane", "USB", "Bourg-en-Bresse", nationale),
				new Team("OM Marcq-en-Barœul", "MAR", "Marcq-en-Barœul", nationale));
	}
}
