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
@Order(4)
public class ForeignCompetitionsDataLoader implements ApplicationRunner {

	private static final LocalDate SEASON_START = LocalDate.of(2026, 8, 1);
	private static final String SEASON = "2026-2027";

	private final CompetitionRepository competitionRepository;
	private final TeamRepository teamRepository;
	private final RugbyMatchRepository rugbyMatchRepository;

	public ForeignCompetitionsDataLoader(
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
		seed("Champions Cup", "ERCC", erccTeams());
		seed("Challenge Cup", "ERCH", erchTeams());
		seed("United Rugby Championship", "URC", urcTeams());
		seed("Premiership", "PREM", premTeams());
		seed("Tests internationaux", "INT", intTeams());
	}

	private void seed(String name, String code, List<TeamSeed> teams) {
		Competition competition = competitionRepository.findByCode(code)
				.map(existing -> {
					existing.setSeason(SEASON);
					existing.setSeasonStart(SEASON_START);
					return existing;
				})
				.orElseGet(() -> competitionRepository.save(
						new Competition(
								name,
								code,
								SEASON,
								SEASON_START,
								7,
								OffensiveBonusRule.TRY_DIFFERENCE,
								3)));
		Map<String, Team> existing = new HashMap<>();
		for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc(code)) {
			existing.put(team.getShortName(), team);
		}
		Set<String> keep = new HashSet<>();
		for (TeamSeed seed : teams) {
			keep.add(seed.shortName());
			if (!existing.containsKey(seed.shortName())) {
				teamRepository.save(new Team(seed.name(), seed.shortName(), seed.city(), competition));
			}
		}
		for (Team team : existing.values()) {
			if (!keep.contains(team.getShortName())
					&& rugbyMatchRepository.countByTeamId(team.getId()) == 0) {
				teamRepository.delete(team);
			}
		}
	}

	private record TeamSeed(String name, String shortName, String city) {
	}

	private static List<TeamSeed> erccTeams() {
		return List.of(
				new TeamSeed("ASM Clermont Auvergne", "ASM", "Clermont"),
				new TeamSeed("Glasgow Warriors", "GLA", "Glasgow"),
				new TeamSeed("Leicester Tigers", "LEIC", "Leicester"),
				new TeamSeed("Leinster", "LEI", "Dublin"),
				new TeamSeed("Section Paloise", "PAU", "Pau"),
				new TeamSeed("Sale Sharks", "SAL", "Sale"),
				new TeamSeed("Connacht", "CON", "Galway"),
				new TeamSeed("Exeter Chiefs", "EXE", "Exeter"),
				new TeamSeed("Stade Rochelais", "LAR", "La Rochelle"),
				new TeamSeed("Lions", "LIO", "Johannesburg"),
				new TeamSeed("Saracens", "SAR", "London"),
				new TeamSeed("Stade Toulousain", "TOU", "Toulouse"),
				new TeamSeed("Union Bordeaux Begles", "UBB", "Bordeaux"),
				new TeamSeed("Bristol Bears", "BRS", "Bristol"),
				new TeamSeed("Gloucester Rugby", "GLO", "Gloucester"),
				new TeamSeed("Munster", "MUN", "Cork"),
				new TeamSeed("Racing 92", "RAC", "Paris"),
				new TeamSeed("Stormers", "STO", "Cape Town"),
				new TeamSeed("Bath Rugby", "BAT", "Bath"),
				new TeamSeed("Bulls", "BUL", "Pretoria"),
				new TeamSeed("Cardiff Rugby", "CDF", "Cardiff"),
				new TeamSeed("Montpellier Herault Rugby", "MHR", "Montpellier"),
				new TeamSeed("Northampton Saints", "NOR", "Northampton"),
				new TeamSeed("Stade Francais Paris", "SFP", "Paris"));
	}

	private static List<TeamSeed> erchTeams() {
		return List.of(
				new TeamSeed("Aviron Bayonnais", "BAY", "Bayonne"),
				new TeamSeed("Cheetahs", "CHE", "Bloemfontein"),
				new TeamSeed("Dragons", "DRA", "Newport"),
				new TeamSeed("USA Perpignan", "USAP", "Perpignan"),
				new TeamSeed("Ulster", "ULS", "Belfast"),
				new TeamSeed("Zebre Parma", "ZEB", "Parma"),
				new TeamSeed("Castres Olympique", "CAS", "Castres"),
				new TeamSeed("LOU Rugby", "LOU", "Lyon"),
				new TeamSeed("Newcastle Falcons", "NEW", "Newcastle"),
				new TeamSeed("Scarlets", "SCA", "Llanelli"),
				new TeamSeed("Sharks", "SHA", "Durban"),
				new TeamSeed("Benetton Treviso", "TRE", "Treviso"),
				new TeamSeed("Black Lion", "BLA", "Tbilisi"),
				new TeamSeed("Edinburgh Rugby", "EDI", "Edinburgh"),
				new TeamSeed("Harlequins", "HAR", "London"),
				new TeamSeed("Ospreys", "OSP", "Swansea"),
				new TeamSeed("RC Toulonnais", "TOL", "Toulon"),
				new TeamSeed("RC Vannes", "VAN", "Vannes"));
	}

	private static List<TeamSeed> urcTeams() {
		return List.of(
				new TeamSeed("Bulls", "BUL", "Pretoria"),
				new TeamSeed("Sharks", "SHA", "Durban"),
				new TeamSeed("Stormers", "STO", "Cape Town"),
				new TeamSeed("Glasgow Warriors", "GLA", "Glasgow"),
				new TeamSeed("Edinburgh Rugby", "EDI", "Edinburgh"),
				new TeamSeed("Cardiff Rugby", "CDF", "Cardiff"),
				new TeamSeed("Lions", "LIO", "Johannesburg"),
				new TeamSeed("Dragons", "DRA", "Newport"),
				new TeamSeed("Benetton Treviso", "TRE", "Treviso"),
				new TeamSeed("Leinster", "LEI", "Dublin"),
				new TeamSeed("Ulster", "ULS", "Belfast"),
				new TeamSeed("Munster", "MUN", "Cork"),
				new TeamSeed("Scarlets", "SCA", "Llanelli"),
				new TeamSeed("Ospreys", "OSP", "Swansea"),
				new TeamSeed("Connacht", "CON", "Galway"),
				new TeamSeed("Zebre Parma", "ZEB", "Parma"));
	}

	private static List<TeamSeed> premTeams() {
		return List.of(
				new TeamSeed("Northampton Saints", "NOR", "Northampton"),
				new TeamSeed("Gloucester Rugby", "GLO", "Gloucester"),
				new TeamSeed("Bath Rugby", "BAT", "Bath"),
				new TeamSeed("Bristol Bears", "BRS", "Bristol"),
				new TeamSeed("Saracens", "SAR", "London"),
				new TeamSeed("Leicester Tigers", "LEIC", "Leicester"),
				new TeamSeed("Exeter Chiefs", "EXE", "Exeter"),
				new TeamSeed("Newcastle Falcons", "NEW", "Newcastle"),
				new TeamSeed("Sale Sharks", "SAL", "Sale"),
				new TeamSeed("Harlequins", "HAR", "London"));
	}

	private static List<TeamSeed> intTeams() {
		return List.of(
				new TeamSeed("Afrique du Sud", "RSA", "Afrique du Sud"),
				new TeamSeed("Afrique du Sud A", "RSA2", "Afrique du Sud"),
				new TeamSeed("All Blacks", "NZL", "Nouvelle-Zelande"),
				new TeamSeed("All Blacks XV", "NZL2", "Nouvelle-Zelande"),
				new TeamSeed("Angleterre A", "ENG2", "Angleterre"),
				new TeamSeed("Argentine", "ARG", "Argentine"),
				new TeamSeed("Argentine XV", "ARG2", "Argentine"),
				new TeamSeed("Australie", "AUS", "Australie"),
				new TeamSeed("Barbarians", "BAR", "Barbarians"),
				new TeamSeed("Canada", "CAN", "Canada"),
				new TeamSeed("Chili", "CHI", "Chili"),
				new TeamSeed("Espagne", "ESP", "Espagne"),
				new TeamSeed("Fidji", "FIJ", "Fidji"),
				new TeamSeed("France A", "FRA2", "France"),
				new TeamSeed("Galles", "WAL", "Galles"),
				new TeamSeed("Georgie", "GEO", "Georgie"),
				new TeamSeed("Hong Kong", "HKG", "Hong Kong"),
				new TeamSeed("Irlande A", "IRL2", "Irlande"),
				new TeamSeed("Italie XV", "ITA2", "Italie"),
				new TeamSeed("Japon", "JPN", "Japon"),
				new TeamSeed("Japon XV", "JPN2", "Japon"),
				new TeamSeed("Maori All Blacks", "MAO", "Nouvelle-Zelande"),
				new TeamSeed("Portugal", "POR", "Portugal"),
				new TeamSeed("Roumanie", "ROUM", "Roumanie"),
				new TeamSeed("Etats-Unis", "USA", "Etats-Unis"),
				new TeamSeed("Uruguay", "URU", "Uruguay"),
				new TeamSeed("Zimbabwe", "ZIM", "Zimbabwe"));
	}
}
