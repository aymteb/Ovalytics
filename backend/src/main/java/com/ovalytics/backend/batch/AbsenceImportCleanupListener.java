package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.domain.Absence;
import com.ovalytics.backend.domain.Team;
import com.ovalytics.backend.repository.AbsenceRepository;
import com.ovalytics.backend.repository.TeamRepository;

@Component
public class AbsenceImportCleanupListener implements JobExecutionListener {

	private static final String[] COMPETITION_CODES = {"TOP14", "PROD2"};

	private final AbsenceRepository absenceRepository;
	private final TeamRepository teamRepository;

	public AbsenceImportCleanupListener(
			AbsenceRepository absenceRepository,
			TeamRepository teamRepository) {
		this.absenceRepository = absenceRepository;
		this.teamRepository = teamRepository;
	}

	@Override
	@Transactional
	public void beforeJob(JobExecution jobExecution) {
		for (String code : COMPETITION_CODES) {
			for (Team team : teamRepository.findByCompetitionCodeOrderByNameAsc(code)) {
				for (Absence absence : absenceRepository.findByTeamId(team.getId())) {
					absenceRepository.delete(absence);
				}
			}
		}
	}
}
