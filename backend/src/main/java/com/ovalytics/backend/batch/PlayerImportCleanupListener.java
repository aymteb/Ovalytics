package com.ovalytics.backend.batch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.domain.MatchAppearance;
import com.ovalytics.backend.domain.Player;
import com.ovalytics.backend.domain.Transfer;
import com.ovalytics.backend.repository.AbsenceRepository;
import com.ovalytics.backend.repository.MatchAppearanceRepository;
import com.ovalytics.backend.repository.PlayerRepository;
import com.ovalytics.backend.repository.TransferRepository;

@Component
public class PlayerImportCleanupListener implements JobExecutionListener {

	private final PlayerImportTracker tracker;
	private final PlayerRepository playerRepository;
	private final TransferRepository transferRepository;
	private final MatchAppearanceRepository matchAppearanceRepository;
	private final AbsenceRepository absenceRepository;

	public PlayerImportCleanupListener(
			PlayerImportTracker tracker,
			PlayerRepository playerRepository,
			TransferRepository transferRepository,
			MatchAppearanceRepository matchAppearanceRepository,
			AbsenceRepository absenceRepository) {
		this.tracker = tracker;
		this.playerRepository = playerRepository;
		this.transferRepository = transferRepository;
		this.matchAppearanceRepository = matchAppearanceRepository;
		this.absenceRepository = absenceRepository;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		tracker.reset();
	}

	@Override
	@Transactional
	public void afterJob(JobExecution jobExecution) {
		Map<Long, Set<String>> imported = tracker.snapshot();
		for (Map.Entry<Long, Set<String>> entry : imported.entrySet()) {
			Long teamId = entry.getKey();
			Set<String> keep = entry.getValue();
			List<Player> squad = playerRepository.findByTeamIdOrderByNameAsc(teamId);
			for (Player player : squad) {
				if (!keep.contains(PlayerImportTracker.normalize(player.getName()))) {
					detachAndDelete(player);
				}
			}
		}
	}

	private void detachAndDelete(Player player) {
		for (Transfer transfer : transferRepository.findByPlayerIdOrderByTransferDateDesc(player.getId())) {
			transfer.setPlayer(null);
		}
		for (MatchAppearance appearance : matchAppearanceRepository
				.findByPlayerIdOrderByKickoffDesc(player.getId())) {
			matchAppearanceRepository.delete(appearance);
		}
		for (Absence absence : absenceRepository.findByPlayerId(player.getId())) {
			absenceRepository.delete(absence);
		}
		playerRepository.delete(player);
	}
}
