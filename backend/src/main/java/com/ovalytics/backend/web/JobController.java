package com.ovalytics.backend.web;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobOperator jobOperator;
	private final Job matchImportJob;

	public JobController(JobOperator jobOperator, Job matchImportJob) {
		this.jobOperator = jobOperator;
		this.matchImportJob = matchImportJob;
	}

	@PostMapping("/match-import")
	public ResponseEntity<String> runMatchImport() throws Exception {
		jobOperator.start(
				matchImportJob,
				new JobParametersBuilder()
						.addLong("run.id", System.currentTimeMillis())
						.toJobParameters());
		return ResponseEntity.ok("Import lance");
	}
}
