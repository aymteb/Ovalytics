package com.ovalytics.backend.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ovalytics.backend.repository.AbsenceRepository;

@Component
public class AbsenceImportCleanupListener implements JobExecutionListener {

	private final AbsenceRepository absenceRepository;

	public AbsenceImportCleanupListener(AbsenceRepository absenceRepository) {
		this.absenceRepository = absenceRepository;
	}

	@Override
	@Transactional
	public void beforeJob(JobExecution jobExecution) {
		absenceRepository.deleteAll();
	}
}
