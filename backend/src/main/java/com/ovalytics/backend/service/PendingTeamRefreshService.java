package com.ovalytics.backend.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.PendingTeamRefresh;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.PendingTeamRefreshRepository;

@Service
public class PendingTeamRefreshService {

	private final PendingTeamRefreshRepository pendingTeamRefreshRepository;

	public PendingTeamRefreshService(PendingTeamRefreshRepository pendingTeamRefreshRepository) {
		this.pendingTeamRefreshRepository = pendingTeamRefreshRepository;
	}

	@Transactional
	public void enqueue(Team team) {
		if (!pendingTeamRefreshRepository.existsByTeamId(team.getId())) {
			pendingTeamRefreshRepository.save(new PendingTeamRefresh(team, Instant.now()));
		}
	}
}
