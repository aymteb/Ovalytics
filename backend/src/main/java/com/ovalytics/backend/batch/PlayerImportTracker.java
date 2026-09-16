package com.ovalytics.backend.batch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PlayerImportTracker {

	private final Map<Long, Set<String>> importedNamesByTeam = new ConcurrentHashMap<>();

	public void reset() {
		importedNamesByTeam.clear();
	}

	public void mark(Long teamId, String playerName) {
		if (teamId == null || playerName == null || playerName.isBlank()) {
			return;
		}
		importedNamesByTeam
				.computeIfAbsent(teamId, id -> ConcurrentHashMap.newKeySet())
				.add(normalize(playerName));
	}

	public Map<Long, Set<String>> snapshot() {
		return Map.copyOf(importedNamesByTeam);
	}

	public static String normalize(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
