package com.ovalytics.backend.service.flashscore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class FlashscoreTeamMapper {

	private static final Map<String, String> TOP14 = Map.ofEntries(
			Map.entry("Stade Toulousain", "TOU"),
			Map.entry("Toulouse", "TOU"),
			Map.entry("Racing 92", "RAC"),
			Map.entry("Racing Metro 92", "RAC"),
			Map.entry("Stade Français", "SFP"),
			Map.entry("Stade Français Paris", "SFP"),
			Map.entry("RC Toulon", "TOL"),
			Map.entry("Toulon", "TOL"),
			Map.entry("Stade Rochelais", "LAR"),
			Map.entry("La Rochelle", "LAR"),
			Map.entry("Union Bordeaux-Bègles", "UBB"),
			Map.entry("Union Bordeaux Begles", "UBB"),
			Map.entry("Union Bordeaux-Begles", "UBB"),
			Map.entry("Bordeaux Bègles", "UBB"),
			Map.entry("Bordeaux Begles", "UBB"),
			Map.entry("ASM Clermont", "ASM"),
			Map.entry("Clermont", "ASM"),
			Map.entry("Lyon OU", "LOU"),
			Map.entry("LOU Rugby", "LOU"),
			Map.entry("Lyon", "LOU"),
			Map.entry("Montpellier HR", "MHR"),
			Map.entry("Montpellier", "MHR"),
			Map.entry("Montpellier Hérault Rugby", "MHR"),
			Map.entry("Castres Olympique", "CAS"),
			Map.entry("Castres", "CAS"),
			Map.entry("Section Paloise", "PAU"),
			Map.entry("Pau", "PAU"),
			Map.entry("Aviron Bayonnais", "BAY"),
			Map.entry("Bayonne", "BAY"),
			Map.entry("USA Perpignan", "USAP"),
			Map.entry("Perpignan", "USAP"),
			Map.entry("RC Vannes", "VAN"),
			Map.entry("Vannes", "VAN"));

	private static final Map<String, String> PROD2 = Map.ofEntries(
			Map.entry("Biarritz Olympique", "BIA"),
			Map.entry("Biarritz", "BIA"),
			Map.entry("Stade Niçois", "NIC"),
			Map.entry("Nissa", "NIC"),
			Map.entry("Nice", "NIC"),
			Map.entry("Stade Niçois Rugby", "NIC"),
			Map.entry("Soyaux Angoulême", "ANG"),
			Map.entry("Angoulême", "ANG"),
			Map.entry("Colomiers Rugby", "COL"),
			Map.entry("Colomiers", "COL"),
			Map.entry("AS Béziers Hérault", "BEZ"),
			Map.entry("AS Béziers", "BEZ"),
			Map.entry("Béziers", "BEZ"),
			Map.entry("Beziers", "BEZ"),
			Map.entry("US Oyonnax", "OYO"),
			Map.entry("Oyonnax Rugby", "OYO"),
			Map.entry("Oyonnax", "OYO"),
			Map.entry("US Dax", "DAX"),
			Map.entry("Dax", "DAX"),
			Map.entry("RC Narbonne", "NAR"),
			Map.entry("Narbonne", "NAR"),
			Map.entry("FC Grenoble", "GRE"),
			Map.entry("Grenoble", "GRE"),
			Map.entry("SA Aurillac", "AUR"),
			Map.entry("Stade Aurillacois", "AUR"),
			Map.entry("Aurillac", "AUR"),
			Map.entry("USON Nevers", "NEV"),
			Map.entry("USO Nevers Rugby", "NEV"),
			Map.entry("Nevers", "NEV"),
			Map.entry("US Montauban", "MTB"),
			Map.entry("Montauban", "MTB"),
			Map.entry("Provence Rugby", "AIX"),
			Map.entry("Aix-en-Provence", "AIX"),
			Map.entry("SU Agen", "AGE"),
			Map.entry("Agen", "AGE"),
			Map.entry("CA Brive", "BRI"),
			Map.entry("Brive", "BRI"),
			Map.entry("Valence Romans", "VAL"),
			Map.entry("Valence Romans Drôme Rugby", "VAL"));

	private final Map<String, String> top14Lookup = buildLookup(TOP14);
	private final Map<String, String> prod2Lookup = buildLookup(PROD2);

	public Optional<TeamPair> mapPair(String homeName, String awayName) {
		if (homeName == null || awayName == null) {
			return Optional.empty();
		}
		if (homeName.toLowerCase(Locale.ROOT).contains("7s")
				|| awayName.toLowerCase(Locale.ROOT).contains("7s")) {
			return Optional.empty();
		}

		Optional<String> top14Home = resolve(top14Lookup, homeName);
		Optional<String> top14Away = resolve(top14Lookup, awayName);
		if (top14Home.isPresent() && top14Away.isPresent()) {
			return Optional.of(new TeamPair("TOP14", top14Home.get(), top14Away.get()));
		}

		Optional<String> prod2Home = resolve(prod2Lookup, homeName);
		Optional<String> prod2Away = resolve(prod2Lookup, awayName);
		if (prod2Home.isPresent() && prod2Away.isPresent()) {
			return Optional.of(new TeamPair("PROD2", prod2Home.get(), prod2Away.get()));
		}

		return Optional.empty();
	}

	private Optional<String> resolve(Map<String, String> lookup, String name) {
		String clean = decode(name).trim();
		String direct = lookup.get(clean);
		if (direct != null) {
			return Optional.of(direct);
		}
		String lower = clean.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> entry : lookup.entrySet()) {
			String label = entry.getKey().toLowerCase(Locale.ROOT);
			if (label.contains(lower) || lower.contains(label)) {
				return Optional.of(entry.getValue());
			}
		}
		return Optional.empty();
	}

	private static Map<String, String> buildLookup(Map<String, String> source) {
		Map<String, String> lookup = new HashMap<>();
		for (Map.Entry<String, String> entry : source.entrySet()) {
			lookup.put(decode(entry.getKey()), entry.getValue());
		}
		return lookup;
	}

	private static String decode(String value) {
		return value
				.replace("&egrave;", "è")
				.replace("&Egrave;", "È")
				.replace("&eacute;", "é")
				.replace("&Eacute;", "É")
				.replace("&ocirc;", "ô")
				.replace("&nbsp;", " ")
				.trim();
	}

	public record TeamPair(String competitionCode, String homeShortName, String awayShortName) {
	}
}
