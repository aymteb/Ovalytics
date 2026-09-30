package com.ovalytics.backend.service.flashscore;

import java.util.HashMap;
import java.util.List;
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

	private static final Map<String, String> NAT = Map.ofEntries(
			Map.entry("RC Massy Essonne", "MAS"),
			Map.entry("Massy", "MAS"),
			Map.entry("US Carcassonne", "CAR"),
			Map.entry("Carcassonne", "CAR"),
			Map.entry("SC Albi", "ALB"),
			Map.entry("Albi", "ALB"),
			Map.entry("Stade Montois", "MDM"),
			Map.entry("Mont-de-Marsan", "MDM"),
			Map.entry("SO Chambéry", "CHA"),
			Map.entry("Chambéry", "CHA"),
			Map.entry("Chambery", "CHA"),
			Map.entry("Rouen Normandie", "ROU"),
			Map.entry("Rouen", "ROU"),
			Map.entry("RC Suresnes", "SUR"),
			Map.entry("Suresnes", "SUR"),
			Map.entry("CS Bourgoin-Jallieu", "BOU"),
			Map.entry("Bourgoin", "BOU"),
			Map.entry("Orléans", "ORL"),
			Map.entry("Orleans", "ORL"),
			Map.entry("CA Périgueux", "PER"),
			Map.entry("Périgueux", "PER"),
			Map.entry("Perigueux", "PER"),
			Map.entry("Rennes", "REN"),
			Map.entry("CS Vienne", "VIE"),
			Map.entry("Vienne", "VIE"),
			Map.entry("US Bressane", "USB"),
			Map.entry("Bourg-en-Bresse", "USB"),
			Map.entry("Ol. Marcquois", "MAR"),
			Map.entry("Marcq-en-Baroeul", "MAR"),
			Map.entry("Marcq-en-Barœul", "MAR"));

	private static final Map<String, String> ERCC = Map.ofEntries(
			Map.entry("Stade Toulousain", "TOU"),
			Map.entry("Toulouse", "TOU"),
			Map.entry("Racing 92", "RAC"),
			Map.entry("Stade Français", "SFP"),
			Map.entry("Stade Rochelais", "LAR"),
			Map.entry("Bordeaux Bègles", "UBB"),
			Map.entry("Union Bordeaux-Bègles", "UBB"),
			Map.entry("ASM Clermont", "ASM"),
			Map.entry("Montpellier", "MHR"),
			Map.entry("Section Paloise", "PAU"),
			Map.entry("Bath", "BAT"),
			Map.entry("Bristol", "BRS"),
			Map.entry("Bulls", "BUL"),
			Map.entry("Cardiff Rugby", "CDF"),
			Map.entry("Cardiff", "CDF"),
			Map.entry("Connacht", "CON"),
			Map.entry("Exeter Chiefs", "EXE"),
			Map.entry("Exeter", "EXE"),
			Map.entry("Glasgow", "GLA"),
			Map.entry("Gloucester", "GLO"),
			Map.entry("Leicester Tigers", "LEIC"),
			Map.entry("Leicester", "LEIC"),
			Map.entry("Leinster", "LEI"),
			Map.entry("Lions", "LIO"),
			Map.entry("Munster", "MUN"),
			Map.entry("Northampton Saints", "NOR"),
			Map.entry("Northampton", "NOR"),
			Map.entry("Sale Sharks", "SAL"),
			Map.entry("Sale", "SAL"),
			Map.entry("Saracens", "SAR"),
			Map.entry("Stormers", "STO"));

	private static final Map<String, String> ERCH = Map.ofEntries(
			Map.entry("Aviron Bayonnais", "BAY"),
			Map.entry("Bayonne", "BAY"),
			Map.entry("Cheetahs", "CHE"),
			Map.entry("Dragons", "DRA"),
			Map.entry("USA Perpignan", "USAP"),
			Map.entry("Perpignan", "USAP"),
			Map.entry("Ulster", "ULS"),
			Map.entry("Zebre", "ZEB"),
			Map.entry("Castres Olympique", "CAS"),
			Map.entry("Castres", "CAS"),
			Map.entry("Lyon OU", "LOU"),
			Map.entry("Lyon", "LOU"),
			Map.entry("Newcastle Red Bulls", "NEW"),
			Map.entry("Newcastle", "NEW"),
			Map.entry("Scarlets", "SCA"),
			Map.entry("Sharks", "SHA"),
			Map.entry("Benetton", "TRE"),
			Map.entry("Black Lion", "BLA"),
			Map.entry("Edinburgh", "EDI"),
			Map.entry("Harlequins", "HAR"),
			Map.entry("Ospreys", "OSP"),
			Map.entry("RC Toulon", "TOL"),
			Map.entry("Toulon", "TOL"),
			Map.entry("RC Vannes", "VAN"),
			Map.entry("Vannes", "VAN"));

	private static final Map<String, String> URC = Map.ofEntries(
			Map.entry("Bulls", "BUL"),
			Map.entry("Sharks", "SHA"),
			Map.entry("Stormers", "STO"),
			Map.entry("Glasgow", "GLA"),
			Map.entry("Edinburgh", "EDI"),
			Map.entry("Cardiff Rugby", "CDF"),
			Map.entry("Cardiff", "CDF"),
			Map.entry("Lions", "LIO"),
			Map.entry("Dragons", "DRA"),
			Map.entry("Benetton", "TRE"),
			Map.entry("Leinster", "LEI"),
			Map.entry("Ulster", "ULS"),
			Map.entry("Munster", "MUN"),
			Map.entry("Scarlets", "SCA"),
			Map.entry("Ospreys", "OSP"),
			Map.entry("Connacht", "CON"),
			Map.entry("Zebre", "ZEB"));

	private static final Map<String, String> PREM = Map.ofEntries(
			Map.entry("Northampton Saints", "NOR"),
			Map.entry("Northampton", "NOR"),
			Map.entry("Gloucester", "GLO"),
			Map.entry("Bath", "BAT"),
			Map.entry("Bristol", "BRS"),
			Map.entry("Saracens", "SAR"),
			Map.entry("Leicester Tigers", "LEIC"),
			Map.entry("Leicester", "LEIC"),
			Map.entry("Exeter Chiefs", "EXE"),
			Map.entry("Exeter", "EXE"),
			Map.entry("Newcastle Red Bulls", "NEW"),
			Map.entry("Newcastle", "NEW"),
			Map.entry("Sale Sharks", "SAL"),
			Map.entry("Sale", "SAL"),
			Map.entry("Harlequins", "HAR"));

	private static final List<String> FALLBACK_ORDER = List.of(
			"TOP14", "NAT", "PROD2", "ERCC", "ERCH", "URC", "PREM");

	private final Map<String, Map<String, String>> lookups = Map.of(
			"TOP14", buildLookup(TOP14),
			"PROD2", buildLookup(PROD2),
			"NAT", buildLookup(NAT),
			"ERCC", buildLookup(ERCC),
			"ERCH", buildLookup(ERCH),
			"URC", buildLookup(URC),
			"PREM", buildLookup(PREM));

	public Optional<TeamPair> mapPair(String homeName, String awayName) {
		return mapPair(homeName, awayName, null);
	}

	public Optional<TeamPair> mapPair(String homeName, String awayName, String preferredCompetition) {
		if (homeName == null || awayName == null) {
			return Optional.empty();
		}
		if (homeName.toLowerCase(Locale.ROOT).contains("7s")
				|| awayName.toLowerCase(Locale.ROOT).contains("7s")) {
			return Optional.empty();
		}
		if (preferredCompetition != null) {
			Optional<TeamPair> preferred = mapInCompetition(preferredCompetition, homeName, awayName);
			if (preferred.isPresent()) {
				return preferred;
			}
		}
		for (String code : FALLBACK_ORDER) {
			Optional<TeamPair> mapped = mapInCompetition(code, homeName, awayName);
			if (mapped.isPresent()) {
				return mapped;
			}
		}
		return Optional.empty();
	}

	private Optional<TeamPair> mapInCompetition(String code, String homeName, String awayName) {
		Map<String, String> lookup = lookups.get(code);
		if (lookup == null) {
			return Optional.empty();
		}
		Optional<String> home = resolve(lookup, homeName);
		Optional<String> away = resolve(lookup, awayName);
		if (home.isPresent() && away.isPresent()) {
			return Optional.of(new TeamPair(code, home.get(), away.get()));
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
