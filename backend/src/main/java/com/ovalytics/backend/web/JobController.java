package com.ovalytics.backend.web;

import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ovalytics.backend.domain.PendingTeamRefresh;
import com.ovalytics.backend.repository.PendingTeamRefreshRepository;
import com.ovalytics.backend.service.MatchAnalysisService;
import com.ovalytics.backend.service.TeamRefreshScraperService;
import com.ovalytics.backend.service.TeamRefreshScheduler;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobOperator jobOperator;
	private final Job matchImportJob;
	private final Job prod2MatchImportJob;
	private final Job newsImportJob;
	private final Job transferImportJob;
	private final Job playerImportJob;
	private final Job playerProfileImportJob;
	private final Job playerAppearanceImportJob;
	private final Job absenceImportJob;
	private final PendingTeamRefreshRepository pendingTeamRefreshRepository;
	private final TeamRefreshScraperService teamRefreshScraperService;
	private final TeamRefreshScheduler teamRefreshScheduler;
	private final MatchAnalysisService matchAnalysisService;

	public JobController(
			JobOperator jobOperator,
			Job matchImportJob,
			Job prod2MatchImportJob,
			Job newsImportJob,
			Job transferImportJob,
			Job playerImportJob,
			Job playerProfileImportJob,
			Job playerAppearanceImportJob,
			Job absenceImportJob,
			PendingTeamRefreshRepository pendingTeamRefreshRepository,
			TeamRefreshScraperService teamRefreshScraperService,
			TeamRefreshScheduler teamRefreshScheduler,
			MatchAnalysisService matchAnalysisService) {
		this.jobOperator = jobOperator;
		this.matchImportJob = matchImportJob;
		this.prod2MatchImportJob = prod2MatchImportJob;
		this.newsImportJob = newsImportJob;
		this.transferImportJob = transferImportJob;
		this.playerImportJob = playerImportJob;
		this.playerProfileImportJob = playerProfileImportJob;
		this.playerAppearanceImportJob = playerAppearanceImportJob;
		this.absenceImportJob = absenceImportJob;
		this.pendingTeamRefreshRepository = pendingTeamRefreshRepository;
		this.teamRefreshScraperService = teamRefreshScraperService;
		this.teamRefreshScheduler = teamRefreshScheduler;
		this.matchAnalysisService = matchAnalysisService;
	}

	@GetMapping("/pending-teams")
	public List<String> pendingTeams() {
		return pendingTeamRefreshRepository.findAllWithTeam().stream()
				.map(entry -> entry.getTeam().getShortName())
				.distinct()
				.sorted()
				.toList();
	}

	@PostMapping("/match-import")
	public ResponseEntity<String> runMatchImport() throws Exception {
		return startJob(matchImportJob);
	}

	@PostMapping("/match-import/prod2")
	public ResponseEntity<String> runProd2MatchImport() throws Exception {
		return startJob(prod2MatchImportJob);
	}

	@PostMapping("/news-import")
	public ResponseEntity<String> runNewsImport() throws Exception {
		return startJob(newsImportJob);
	}

	@PostMapping("/transfer-import")
	public ResponseEntity<String> runTransferImport() throws Exception {
		return startJob(transferImportJob);
	}

	@PostMapping("/squad-import")
	public ResponseEntity<String> runSquadImport() throws Exception {
		return startJob(playerImportJob);
	}

	@PostMapping("/player-profile-import")
	public ResponseEntity<String> runPlayerProfileImport() throws Exception {
		return startJob(playerProfileImportJob);
	}

	@PostMapping("/player-appearance-import")
	public ResponseEntity<String> runPlayerAppearanceImport() throws Exception {
		return startJob(playerAppearanceImportJob);
	}

	@PostMapping("/absence-import")
	public ResponseEntity<String> runAbsenceImport() throws Exception {
		return startJob(absenceImportJob);
	}

	@PostMapping("/analysis-generate")
	public ResponseEntity<String> runAnalysisGenerate(
			@RequestParam(required = false) Long matchId) {
		if (matchId != null) {
			matchAnalysisService.generateForMatch(matchId);
			return ResponseEntity.ok("Analyse generee pour match " + matchId);
		}
		int count = matchAnalysisService.generateForUpcomingWindow();
		return ResponseEntity.ok(count + " analyse(s) generee(s)");
	}

	@PostMapping("/team-refresh")
	public ResponseEntity<String> runTeamRefresh() {
		teamRefreshScheduler.processPendingTeams();
		return ResponseEntity.ok("Refresh equipes lance");
	}

	@PostMapping("/scrape-player-profiles")
	public ResponseEntity<String> scrapePlayerProfiles() {
		List<String> teams = pendingTeamRefreshRepository.findAllWithTeam().stream()
				.map(entry -> entry.getTeam().getShortName())
				.distinct()
				.toList();
		if (teams.isEmpty()) {
			return ResponseEntity.badRequest().body("Aucun club en file d'attente");
		}
		boolean ok = teamRefreshScraperService.scrapeProfiles(teams);
		if (!ok) {
			return ResponseEntity.internalServerError().body("Scrape en echec");
		}
		return ResponseEntity.ok("Scrape lance pour " + String.join(", ", teams));
	}

	private ResponseEntity<String> startJob(Job job) throws Exception {
		jobOperator.start(
				job,
				new JobParametersBuilder()
						.addLong("run.id", System.currentTimeMillis())
						.toJobParameters());
		return ResponseEntity.ok("Import lance");
	}
}
