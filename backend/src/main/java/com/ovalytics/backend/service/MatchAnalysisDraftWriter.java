package com.ovalytics.backend.service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ovalytics.backend.web.dto.AbsenceResponse;
import com.ovalytics.backend.web.dto.HeadToHeadMatchResponse;
import com.ovalytics.backend.web.dto.MatchResponse;
import com.ovalytics.backend.web.dto.StandingRowResponse;
import com.ovalytics.backend.web.dto.TeamFormResponse;
import com.ovalytics.backend.web.dto.TeamResponse;
import com.ovalytics.backend.web.dto.VenueRecordResponse;

@Component
public class MatchAnalysisDraftWriter {

	private static final Map<String, String> STADIUMS = Map.ofEntries(
			Map.entry("VAN", "la Rabine"),
			Map.entry("TOU", "Ernest-Wallon"),
			Map.entry("UBB", "Chaban-Delmas"),
			Map.entry("RAC", "Paris La Défense Arena"),
			Map.entry("LAR", "Marcel-Deflandre"),
			Map.entry("TOL", "Mayol"),
			Map.entry("ASM", "Michelin"),
			Map.entry("LOU", "Gerland"),
			Map.entry("SFP", "Jean-Bouin"),
			Map.entry("MHR", "GGL Stadium"),
			Map.entry("CAS", "Pierre-Fabre"),
			Map.entry("PAU", "du Hameau"),
			Map.entry("BAY", "Jean-Dauger"),
			Map.entry("USAP", "Aimé-Giral"),
			Map.entry("MTB", "Sapiac"),
			Map.entry("BEZ", "Raoul-Barrière"),
			Map.entry("NEV", "le Pré-Fleuri"),
			Map.entry("BIA", "Aguiléra"),
			Map.entry("GRE", "les Alpes"),
			Map.entry("NIC", "des Digues"),
			Map.entry("OYO", "Charles-Mathon"),
			Map.entry("COL", "Michel-Bendichou"),
			Map.entry("AIX", "Maurice-David"),
			Map.entry("DAX", "Maurice-Boyau"),
			Map.entry("ANG", "Chanzy"),
			Map.entry("AUR", "Jean-Alric"),
			Map.entry("AGE", "Armandie"),
			Map.entry("BRI", "Amédée-Domenech"),
			Map.entry("VAL", "des Gaulois"),
			Map.entry("NAR", "du Parc des Sports"),
			Map.entry("PRO", "du Hameau"));

	public String write(MatchResponse match, MatchAnalysisContext context) {
		long seed = match.id() == null ? 0L : match.id();
		double homeScore = scoreHome(match, context);
		double awayScore = scoreAway(match, context);
		double gap = homeScore - awayScore;
		boolean blowoutAway = gap <= -1.2;
		boolean homeEdge = gap >= 0.2;
		boolean awayEdge = gap <= -0.2;

		StringBuilder text = new StringBuilder();
		text.append(opening(match, blowoutAway, seed));
		text.append(homeBeat(match, context, blowoutAway, seed));
		text.append(awayBeat(match, context, blowoutAway, seed));
		text.append(h2hBeat(match, match.kickoffAt(), seed));
		text.append(absencesBeat(match, seed));
		text.append(battleBeat(blowoutAway, homeEdge, seed));
		text.append(verdict(match, context, blowoutAway, homeEdge, awayEdge, gap, seed));
		return text.toString().trim();
	}

	private static String opening(MatchResponse match, boolean blowoutAway, long seed) {
		String homeShort = match.homeTeam().shortName();
		String home = naturalName(match.homeTeam());
		String away = naturalName(match.awayTeam());
		String stadium = STADIUMS.get(homeShort);

		if (stadium != null && "MTB".equals(homeShort)) {
			return home
					+ " reçoit "
					+ away
					+ " dans la mythique cuvette de "
					+ stadium
					+ " "
					+ pick(seed, 1,
							"pour un duel de caractère.",
							"pour un choc de packs annoncé.",
							"avec l'envie d'imposer sa loi à domicile.")
					+ " ";
		}
		if (stadium != null && blowoutAway) {
			return pick(seed, 2,
					"Le stade de " + stadium + " s'apprête à vivre un sommet d'intensité avec la réception "
							+ deClub(match.awayTeam()) + ". ",
					"Ambiance de grand soir attendue " + stadiumPrep(stadium) + " pour la venue "
							+ deClub(match.awayTeam()) + ". ",
					"La pelouse de " + stadium.replaceFirst("^(le |la |les )", "")
							+ " s'annonce brûlante pour recevoir " + naturalName(match.awayTeam()) + ". ");
		}
		if (stadium != null) {
			return home
					+ " reçoit "
					+ away
					+ " "
					+ stadiumPrep(stadium)
					+ " "
					+ pick(seed, 3,
							"pour un rendez-vous à enjeu.",
							"avec l'obligation de faire parler le terrain.",
							"pour un bras de fer qui promet d'être tendu.",
							"dans un match où chaque détail peut compter.")
					+ " ";
		}
		return home
				+ " reçoit "
				+ away
				+ " "
				+ pick(seed, 4,
						"pour un duel de caractère.",
						"pour un rendez-vous à enjeu.",
						"avec l'envie de faire parler le terrain.")
				+ " ";
	}

	private static String homeBeat(
			MatchResponse match,
			MatchAnalysisContext context,
			boolean blowoutAway,
			long seed) {
		String homeShort = match.homeTeam().shortName();
		String locals = people(homeShort);
		StandingRowResponse standing = context.homeStanding();
		TeamFormResponse form = match.homeForm();
		VenueRecordResponse homeHome = match.homeHomeRecord();

		if (blowoutAway) {
			return pick(seed, 41,
					"Porté par un public incandescent, "
							+ locals
							+ " aborde"
							+ pluralVerb(locals)
							+ " ce rendez-vous sans le moindre complexe, mais le défi relève du monumental face au collectif adverse. ",
					"Devant leur public, "
							+ locals
							+ " n'ont pas l'intention de se cacher, même si l'adversaire affiche un autre standing. ",
					"Chez eux, "
							+ locals
							+ " peuvent rêver d'exploit, à condition d'accepter un combat d'une intensité rare. ");
		}

		if (standing != null && standing.won() == 0 && standing.played() > 0) {
			String place = standing.position() > 0
					? " et coincés à la " + standing.position() + positionSuffix(standing.position()) + " place"
					: "";
			if (context.homeNarrowLossCount() >= 2) {
				String margins = numberWord(context.homeNarrowLossCount())
						+ " courtes défaites, concédées pour un total de seulement "
						+ numberWord(context.homeNarrowLossMarginSum())
						+ " points d'écart cumulés";
				return pick(seed, 42,
						"Toujours sans la moindre victoire"
								+ place
								+ ", "
								+ locals
								+ " n'ont pourtant jamais été loin du sujet : le club reste sur "
								+ margins
								+ ". ",
						"Zéro succès au compteur"
								+ place
								+ " pour "
								+ locals
								+ ", qui accumulent pourtant "
								+ margins
								+ ". ",
						locals.substring(0, 1).toUpperCase(Locale.ROOT)
								+ locals.substring(1)
								+ " cherchent encore leur première victoire"
								+ place
								+ ", après "
								+ margins
								+ ". ");
			}
			return pick(seed, 43,
					"Toujours sans la moindre victoire"
							+ place
							+ ", "
							+ locals
							+ " cherchent encore à débloquer leur compteur. ",
					"Le compteur de victoires reste bloqué à zéro"
							+ place
							+ " : "
							+ locals
							+ " ont besoin d'un déclic. ",
					capitalize(locals)
							+ " n'ont toujours pas gagné"
							+ place
							+ " et abordent l'affiche avec une vraie urgence. ");
		}

		boolean strongHome = homeHome != null && homeHome.played() >= 3 && winRate(homeHome) >= 0.55;
		boolean unfinishedBusiness = form != null && form.played() > 0 && form.won() <= form.lost();
		if (strongHome) {
			return pick(seed, 44,
					capitalize(locals)
							+ " s'appuient sur leur pragmatisme à domicile et une conquête agressive pour dicter le rythme"
							+ (unfinishedBusiness
									? ", même s'ils peinent encore à valider leurs temps forts au tableau d'affichage"
									: "")
							+ ". ",
					"À domicile, "
							+ locals
							+ " savent imposer un rythme rugueux"
							+ (unfinishedBusiness
									? ", malgré quelques frustrations récentes dans la finition"
									: "")
							+ ". ",
					"La force de "
							+ locals
							+ " reste leur capacité à faire parler le terrain"
							+ (unfinishedBusiness
									? ", à condition de transformer enfin leurs périodes de domination"
									: "")
							+ ". ");
		}

		if (form != null && form.played() > 0 && (double) form.won() / form.played() >= 0.6) {
			return pick(seed, 45,
					capitalize(locals)
							+ " abordent l'affiche avec une dynamique rassurante et l'envie d'imposer leur rythme chez eux. ",
					"Le moral est plutôt bon côté "
							+ locals
							+ ", portés par des sorties récentes convaincantes. ",
					capitalize(locals)
							+ " arrivent en confiance et veulent enfoncer le clou devant leur public. ");
		}

		return pick(seed, 46,
				capitalize(locals)
						+ " veulent faire de leur pelouse un vrai levier pour basculer le bras de fer. ",
				"Chez eux, "
						+ locals
						+ " comptent sur l'ambiance et l'intensité pour basculer le match. ",
				capitalize(locals)
						+ " savent que le terrain peut faire la différence dans une affiche aussi serrée. ");
	}

	private static String awayBeat(
			MatchResponse match,
			MatchAnalysisContext context,
			boolean blowoutAway,
			long seed) {
		String awayShort = match.awayTeam().shortName();
		String visitors = people(awayShort);
		VenueRecordResponse awayAway = match.awayAwayRecord();
		MatchAnalysisContext.ScoredOuting lastAway = context.visitorLastAway();

		if (blowoutAway) {
			return pick(seed, 51,
					"Même si le staff adverse peut faire tourner sur certaines lignes pour ce voyage, "
							+ "la profondeur de banc et la vitesse d'exécution des visiteurs n'ont souvent aucun équivalent "
							+ "dans "
							+ match.competitionName()
							+ ". ",
					"La qualité individuelle et collective des visiteurs reste d'un autre calibre, "
							+ "même avec une feuille de match un peu remaniée. ",
					"Face à une telle densité de talent, "
							+ visitors
							+ " ont les moyens d'accélérer dès que le match s'ouvre. ");
		}

		boolean fragileAway = awayAway != null && awayAway.played() >= 3 && winRate(awayAway) <= 0.3;
		if (fragileAway && lastAway != null && lastAway.heavyDefeat()) {
			String score = lastAway.opponentScore() + "-" + lastAway.teamScore();
			String place = cityHint(lastAway.opponentShortName(), lastAway.opponentName());
			String alone = context.visitorAwayGamesThisSeason() == 1
					? " lors de leur seul déplacement de la saison"
					: "";
			return pick(seed, 52,
					"En face, "
							+ visitors
							+ " débarquent avec de solides certitudes à domicile, mais d'énormes doutes à l'extérieur "
							+ "après un lourd naufrage à "
							+ place
							+ " ("
							+ score
							+ ")"
							+ alone
							+ ". ",
					"Le déplacement reste le point faible de "
							+ visitors
							+ ", encore marqués par la correction reçue à "
							+ place
							+ " ("
							+ score
							+ ")"
							+ alone
							+ ". ",
					visitors.substring(0, 1).toUpperCase(Locale.ROOT)
							+ visitors.substring(1)
							+ " voyagent avec des doutes après le "
							+ score
							+ " concédé à "
							+ place
							+ alone
							+ ". ");
		}

		if (fragileAway) {
			return pick(seed, 53,
					"En face, "
							+ visitors
							+ " se déplacent avec de sérieuses ambitions et un jeu de transition capable de punir la moindre erreur, "
							+ "mais restent régulièrement plombés par une indiscipline rédhibitoire loin de leurs bases. ",
					visitors.substring(0, 1).toUpperCase(Locale.ROOT)
							+ visitors.substring(1)
							+ " peuvent faire mal dès qu'ils trouvent de l'espace, "
							+ "mais leur régularité à l'extérieur laisse encore à désirer. ",
					"Loin de chez eux, "
							+ visitors
							+ " ont souvent du mal à transformer leurs intentions en points, "
							+ "malgré un vrai potentiel offensif. ");
		}

		StandingRowResponse awayStanding = context.awayStanding();
		if (awayStanding != null && awayStanding.won() > awayStanding.lost()) {
			return pick(seed, 54,
					"En face, "
							+ visitors
							+ " abordent le déplacement avec de sérieuses ambitions, portés par une meilleure dynamique récente, "
							+ "quitte à devoir gérer une pelouse hostile. ",
					"Le début de saison sourit davantage à "
							+ visitors
							+ ", qui viennent ici pour confirmer leur bonne dynamique. ",
					visitors.substring(0, 1).toUpperCase(Locale.ROOT)
							+ visitors.substring(1)
							+ " arrivent avec du crédit sur ce début de championnat, "
							+ "même sur une pelouse difficile. ");
		}

		return pick(seed, 55,
				"En face, "
						+ visitors
						+ " viennent chercher un résultat loin de leurs bases, en comptant sur leur capacité à punir la moindre erreur. ",
				visitors.substring(0, 1).toUpperCase(Locale.ROOT)
						+ visitors.substring(1)
						+ " n'ont pas fait le déplacement pour se contenter d'exister : ils viseront le moindre espace. ",
				"Les visiteurs comptent sur leur capacité à faire mal dès qu'une porte s'ouvre, "
						+ "malgré le contexte hostile. ");
	}

	private static String h2hBeat(MatchResponse match, LocalDateTime reference, long seed) {
		List<HeadToHeadMatchResponse> h2h = match.headToHead();
		if (h2h == null || h2h.isEmpty()) {
			return "";
		}
		HeadToHeadMatchResponse last = h2h.get(0);
		if (last.homeScore() == null || last.awayScore() == null) {
			return "";
		}

		String homeShort = match.homeTeam().shortName();
		String awayShort = match.awayTeam().shortName();
		String when = whenPhrase(last.kickoffAt(), reference);
		String scoreHomeFirst = last.homeScore() + "-" + last.awayScore();
		String scoreAwayFirst = last.awayScore() + "-" + last.homeScore();

		boolean homeWonAway = last.awayShortName().equals(homeShort)
				&& last.awayScore() > last.homeScore();
		if (homeWonAway) {
			String city = cityHint(awayShort, match.awayTeam().name());
			return pick(seed, 62,
					"Le souvenir de la victoire "
							+ adjective(homeShort)
							+ " à "
							+ city
							+ " ("
							+ scoreAwayFirst
							+ ")"
							+ when
							+ " pèsera inévitablement dans la préparation adverse. ",
					"Les locaux n'ont pas oublié leur "
							+ scoreAwayFirst
							+ " arraché à "
							+ city
							+ when
							+ ". ",
					"Un succès "
							+ scoreAwayFirst
							+ " à "
							+ city
							+ when
							+ " reste un bon point de repère psychologique pour les locaux. ");
		}

		boolean homeWonHome = last.homeShortName().equals(homeShort)
				&& last.homeScore() > last.awayScore();
		if (homeWonHome) {
			String home = naturalName(match.homeTeam());
			return pick(seed, 63,
					"Le dernier face-à-face à domicile avait penché pour "
							+ home
							+ " ("
							+ scoreHomeFirst
							+ ")"
							+ when
							+ ". ",
					"Chez eux, "
							+ home
							+ " s'était déjà imposé "
							+ scoreHomeFirst
							+ when
							+ " face au même adversaire. ",
					"Le précédent à domicile ("
							+ scoreHomeFirst
							+ when
							+ ") avait tourné à l'avantage de "
							+ home
							+ ". ");
		}

		boolean awayWon = (last.homeShortName().equals(awayShort) && last.homeScore() > last.awayScore())
				|| (last.awayShortName().equals(awayShort) && last.awayScore() > last.homeScore());
		if (awayWon) {
			String away = naturalName(match.awayTeam());
			return pick(seed, 64,
					"Le dernier face-à-face avait penché pour "
							+ away
							+ " ("
							+ scoreHomeFirst
							+ ")"
							+ when
							+ ". ",
					away
							+ " avait pris le dessus "
							+ scoreHomeFirst
							+ when
							+ " lors de la dernière confrontation. ",
					"Le précédent ("
							+ scoreHomeFirst
							+ when
							+ ") reste un bon souvenir pour "
							+ away
							+ ". ");
		}
		return pick(seed, 65,
				"Le dernier face-à-face s'était achevé sur un match nul" + when + ". ",
				"La dernière opposition s'était soldée par un partage des points" + when + ". ",
				"Aucun des deux camps n'avait réussi à faire la différence" + when + ". ");
	}

	static String whenPhrase(LocalDateTime event, LocalDateTime reference) {
		String month = event.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
		long months = ChronoUnit.MONTHS.between(event.toLocalDate().withDayOfMonth(1),
				reference.toLocalDate().withDayOfMonth(1));
		if (months >= 0 && months <= 12) {
			return " en " + month + " dernier";
		}
		return " en " + month + " " + event.getYear();
	}

	private static String absencesBeat(MatchResponse match, long seed) {
		List<AbsenceResponse> homeAbsences = match.homeAbsences();
		List<AbsenceResponse> awayAbsences = match.awayAbsences();
		boolean homeHas = homeAbsences != null && !homeAbsences.isEmpty();
		boolean awayHas = awayAbsences != null && !awayAbsences.isEmpty();
		if (!homeHas && !awayHas) {
			return "";
		}
		if (homeHas && awayHas) {
			return pick(seed, 11,
					"Les deux staffs doivent aussi composer avec des absences qui pèsent : "
							+ names(homeAbsences) + " côté locaux, " + names(awayAbsences) + " côté visiteurs. ",
					"Les absences vont peser des deux côtés : "
							+ names(homeAbsences) + " manquent à l'appel chez les locaux, "
							+ names(awayAbsences) + " côté visiteurs. ",
					"Il faudra faire sans quelques éléments importants : "
							+ names(homeAbsences) + " pour les locaux, "
							+ names(awayAbsences) + " pour les visiteurs. ");
		}
		if (homeHas) {
			return pick(seed, 12,
					"Le staff local doit composer sans "
							+ names(homeAbsences)
							+ ", ce qui peut freiner certains automatismes dans les phases clés. ",
					"Sans " + names(homeAbsences)
							+ ", les locaux devront trouver d'autres solutions dans les moments chauds. ",
					"L'absence de " + names(homeAbsences)
							+ " prive le collectif local d'une part de ses automatismes. ");
		}
		return pick(seed, 13,
				"Même si le staff adverse fait parfois tourner, l'absence de "
						+ names(awayAbsences)
						+ " reste un paramètre à surveiller. ",
				"Les visiteurs devront aussi faire sans "
						+ names(awayAbsences)
						+ ", un détail qui peut freiner leurs ambitions. ",
				"Côté visiteurs, l'absence de "
						+ names(awayAbsences)
						+ " ajoute une incertitude sur la physionomie du match. ");
	}

	private static String battleBeat(boolean blowoutAway, boolean homeEdge, long seed) {
		if (blowoutAway) {
			return pick(seed, 21,
					"Les locaux devront livrer une prestation parfaite sur les fondamentaux, "
							+ "notamment une conquête propre et une discipline de fer, pour espérer rivaliser plus d'une mi-temps. "
							+ "La marche est toutefois trop haute face à un collectif qui punit la moindre perte de balle. ",
					"Il faudra une conquête propre et une discipline de fer pour tenir plus d'une mi-temps. "
							+ "Au-delà, la différence d'impact et de profondeur de banc devrait peser très lourd. ",
					"Les fondamentaux devront être parfaits côté local pour exister. "
							+ "La moindre perte de balle risque d'être punie sans appel par des visiteurs ultra-rodés. ");
		}
		if (homeEdge) {
			return pick(seed, 22,
					"Le bras de fer va inévitablement se jouer dans les rucks et sur les ballons portés, "
							+ "où le pack adverse devra résister à la pression constante des locaux pour exister. ",
					"Le match devrait se gagner près des rucks et dans la capacité à faire avancer le ballon porté. ",
					"L'occupation et la conquête s'annoncent décisives : celui qui imposera son rythme dans ces secteurs prendra l'ascendant. ",
					"Attendons-nous à un combat de packs, où chaque duel au sol peut faire basculer la physionomie de la rencontre. ");
		}
		return pick(seed, 23,
				"Le duel devrait se jouer sur l'occupation, la discipline et la capacité à transformer les temps forts. ",
				"Tout se jouera sans doute sur la gestion du territory game et la réussite dans les moments clés. ",
				"La discipline et la précision près des 22 mètres devraient faire la différence. ");
	}

	private static String verdict(
			MatchResponse match,
			MatchAnalysisContext context,
			boolean blowoutAway,
			boolean homeEdge,
			boolean awayEdge,
			double gap,
			long seed) {
		if (blowoutAway) {
			return pick(seed, 31,
					"Après un premier acte accroché où l'engagement des locaux fera illusion, "
							+ "les visiteurs devraient accélérer à l'heure de jeu sous l'impulsion de leurs finisseurs. "
							+ "On s'attend à une victoire nette et sans bavure des visiteurs, "
							+ "qui devraient repartir avec le bonus offensif et une marge supérieure à 15 points.",
					"Les locaux peuvent accrocher le débat une mi-temps, mais la dynamite adverse devrait parler ensuite. "
							+ "Pronostic : succès large des visiteurs, très probablement assorti du bonus offensif.",
					"Une fois le verrou local desserré, les visiteurs ont les armes pour faire sauter le match. "
							+ "On voit mal autre chose qu'une victoire nette à l'extérieur, au-delà des quinze points d'écart.");
		}

		if (context.homeStanding() != null
				&& context.homeStanding().won() == 0
				&& homeEdge) {
			String locals = people(match.homeTeam().shortName());
			return pick(seed, 32,
					"On s'attend à un bras de fer très âpre où "
							+ locals
							+ " devraient logiquement débloquer leur compteur victoire au forceps, "
							+ "très certainement dans les toutes dernières minutes.",
					"Le scénario le plus crédible reste une victoire accrochée des locaux, "
							+ "enfin capables de transformer leur domination en quatre points, sans doute sur le fil.",
					"Si le public pousse jusqu'au bout, "
							+ locals
							+ " ont les arguments pour faire tomber le premier succès de la saison, "
							+ "dans un final tendu.");
		}

		if (homeEdge && gap < 1.4) {
			return pick(seed, 33,
					"On s'attend à une rencontre rugueuse et fermée, où la précision au pied côté local "
							+ "devrait faire la différence dans les dix dernières minutes pour assurer un succès précieux aux locaux, "
							+ "laissant très probablement un bonus défensif aux visiteurs.",
					"Match de caractère en vue : peu d'essais, beaucoup de combat, et une décision qui peut arriver tard. "
							+ "Avantage aux locaux, avec un bonus défensif plausible pour les visiteurs.",
					"Ça sent le score serré et le scénario étouffé. "
							+ "Les locaux devraient finir par l'emporter, sans forcément mettre les visiteurs hors du bonus.");
		}

		if (homeEdge) {
			return pick(seed, 34,
					"On s'attend à une confrontation maîtrisée par les locaux, "
							+ "sans pour autant promettre un festival d'essais.",
					"Les locaux ont assez d'arguments pour garder la main, "
							+ "dans un match plus maîtrisé que spectaculaire.",
					"Pronostic : succès à domicile, obtenu dans la gestion plus que dans le panache.",
					"Le terrain devrait parler : avantage aux locaux, avec une marge raisonnable plutôt qu'un carton.");
		}

		if (awayEdge) {
			return pick(seed, 35,
					"On s'attend à voir les visiteurs prendre l'ascendant après l'heure de jeu, "
							+ "même si le détail peut encore tout basculer.",
					"Les visiteurs partent avec un léger crédit logique, "
							+ "à condition de ne pas se laisser emporter par l'ambiance du stade.",
					"Si le match s'étire, la fraîcheur et l'efficacité adverses devraient faire pencher la balance à l'extérieur.");
		}

		return pick(seed, 36,
				"On s'attend à un match ouvert et très disputé, sans favori vraiment tranché.",
				"Affiche 50-50 : le détail, la discipline ou un tournant d'arbitrage peut tout décider.",
				"Difficile de trancher : on sent un match de rupture, où le premier qui pliera mentalement paiera cash.");
	}

	private static String pick(long seed, int salt, String... options) {
		int index = Math.floorMod((int) (seed * 31L + salt * 17L), options.length);
		return options[index];
	}

	static String pickFavorite(MatchResponse match) {
		return pickFavorite(match, new MatchAnalysisContext(null, null, List.of(), null, 0, 0));
	}

	static String pickFavorite(MatchResponse match, MatchAnalysisContext context) {
		double homeScore = scoreHome(match, context);
		double awayScore = scoreAway(match, context);
		if (Math.abs(homeScore - awayScore) < 0.15) {
			return match.homeTeam().shortName() + " (serré)";
		}
		return homeScore >= awayScore
				? match.homeTeam().shortName()
				: match.awayTeam().shortName();
	}

	private static double scoreHome(MatchResponse match, MatchAnalysisContext context) {
		double score = 1.0;
		score += formStrength(match.homeForm());
		score -= absencePenalty(match.homeAbsences());
		if (match.homeHomeRecord() != null && match.homeHomeRecord().played() > 0) {
			score += winRate(match.homeHomeRecord()) * 1.2;
		}
		if (context != null && context.homeStanding() != null && context.homeStanding().won() == 0) {
			score += 0.15;
		}
		return score;
	}

	private static double scoreAway(MatchResponse match, MatchAnalysisContext context) {
		double score = 0.0;
		score += formStrength(match.awayForm());
		score -= absencePenalty(match.awayAbsences());
		if (match.awayAwayRecord() != null && match.awayAwayRecord().played() > 0) {
			score += winRate(match.awayAwayRecord());
		}
		if (context != null
				&& context.visitorLastAway() != null
				&& context.visitorLastAway().heavyDefeat()) {
			score -= 0.25;
		}
		return score;
	}

	private static double formStrength(TeamFormResponse form) {
		if (form == null || form.played() == 0) {
			return 0.0;
		}
		return winRate(form.won(), form.played()) * 2.0;
	}

	private static double absencePenalty(List<AbsenceResponse> absences) {
		if (absences == null || absences.isEmpty()) {
			return 0.0;
		}
		double penalty = 0.0;
		for (AbsenceResponse absence : absences) {
			String type = absence.type() == null ? "" : absence.type().toUpperCase(Locale.ROOT);
			penalty += switch (type) {
				case "SUSPENDED" -> 0.35;
				case "INJURED" -> 0.25;
				case "INTERNATIONAL" -> 0.2;
				default -> 0.15;
			};
		}
		return Math.min(penalty, 1.2);
	}

	private static double winRate(VenueRecordResponse record) {
		return winRate(record.won(), record.played());
	}

	private static double winRate(int won, int played) {
		if (played <= 0) {
			return 0.0;
		}
		return (double) won / played;
	}

	private static String names(List<AbsenceResponse> absences) {
		return absences.stream()
				.limit(3)
				.map(AbsenceResponse::playerName)
				.collect(Collectors.joining(", "));
	}

	private static String stadiumPrep(String stadium) {
		if (stadium.startsWith("le ")) {
			return "au " + stadium.substring(3);
		}
		if (stadium.startsWith("la ")) {
			return "à " + stadium;
		}
		if (stadium.startsWith("les ")) {
			return "aux " + stadium.substring(4);
		}
		if (stadium.startsWith("du ") || stadium.startsWith("des ")) {
			return "au stade " + stadium;
		}
		return "au stade " + stadium;
	}

	private static String naturalName(TeamResponse team) {
		return switch (team.shortName()) {
			case "TOU" -> "Toulouse";
			case "SFP" -> "le Stade Français";
			case "RAC" -> "le Racing";
			case "UBB" -> "Bordeaux";
			case "LAR" -> "La Rochelle";
			case "TOL" -> "Toulon";
			case "ASM" -> "Clermont";
			case "LOU" -> "Lyon";
			case "MHR" -> "Montpellier";
			case "CAS" -> "Castres";
			case "PAU" -> "Pau";
			case "BAY" -> "Bayonne";
			case "USAP" -> "Perpignan";
			case "VAN" -> "Vannes";
			case "AIX" -> "Provence";
			case "BEZ" -> "Béziers";
			case "MTB" -> "Montauban";
			case "NEV" -> "Nevers";
			case "BIA" -> "Biarritz";
			case "NIC" -> "Nice";
			case "GRE" -> "Grenoble";
			case "OYO" -> "Oyonnax";
			case "COL" -> "Colomiers";
			case "DAX" -> "Dax";
			case "ANG" -> "Angoulême";
			case "AUR" -> "Aurillac";
			case "AGE" -> "Agen";
			case "BRI" -> "Brive";
			case "NAR" -> "Narbonne";
			case "VAL" -> "Valence Romans";
			default -> {
				if (team.city() != null && !team.city().isBlank()) {
					yield team.city();
				}
				yield stripLegalPrefix(team.name());
			}
		};
	}

	private static String stripLegalPrefix(String name) {
		if (name == null || name.isBlank()) {
			return "l'adversaire";
		}
		return name.replaceFirst("(?i)^(USON|US|AS|RC|FC|SU|CA|Stade)\\s+", "").trim();
	}

	private static String deClub(TeamResponse team) {
		return switch (team.shortName()) {
			case "TOU" -> "du Stade Toulousain";
			case "SFP" -> "du Stade Français";
			case "RAC" -> "du Racing";
			case "UBB" -> "de Bordeaux";
			case "LAR" -> "de La Rochelle";
			case "TOL" -> "de Toulon";
			default -> "de " + naturalName(team).replaceFirst("^le ", "");
		};
	}

	private static String people(String shortName) {
		return switch (shortName) {
			case "NEV" -> "les Nivernais";
			case "BIA" -> "les Basques";
			case "NIC" -> "les Niçois";
			case "GRE" -> "les Grenoblois";
			case "TOU" -> "les Toulousains";
			case "UBB" -> "les Bordelais";
			case "RAC" -> "le Racing";
			case "LAR" -> "les Rochelais";
			case "TOL" -> "les Toulonnais";
			case "ASM" -> "les Clermontois";
			case "LOU" -> "les Lyonnais";
			case "SFP" -> "les Parisiens";
			case "MHR" -> "les Montpelliérains";
			case "CAS" -> "les Castrais";
			case "PAU" -> "les Palois";
			case "BAY" -> "les Bayonnais";
			case "VAN" -> "Vannes";
			case "MTB" -> "les Montalbanais";
			case "BEZ" -> "les Biterrois";
			case "OYO" -> "les Oyonnaxiens";
			case "COL" -> "les Columérins";
			case "AIX" -> "Provence";
			case "DAX" -> "les Dacquois";
			case "ANG" -> "les Charentais";
			case "AUR" -> "les Cantalous";
			case "AGE" -> "les Agenais";
			case "BRI" -> "les Brivistes";
			case "VAL" -> "Valence Romans";
			case "NAR" -> "les Narbonnais";
			case "USAP" -> "Perpignan";
			default -> "les locaux";
		};
	}

	private static String adjective(String shortName) {
		return switch (shortName) {
			case "NEV" -> "nivernaise";
			case "BIA" -> "basque";
			case "NIC" -> "niçoise";
			case "GRE" -> "grenobloise";
			case "TOU" -> "toulousaine";
			case "MTB" -> "montalbanaise";
			case "BEZ" -> "biterroise";
			case "VAN" -> "vannetaise";
			default -> "locale";
		};
	}

	private static String pluralVerb(String subject) {
		if (subject.startsWith("les ") || subject.startsWith("Les ")) {
			return "nt";
		}
		return "";
	}

	private static String capitalize(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static String cityHint(String shortName, String fullName) {
		return switch (shortName) {
			case "BIA" -> "Biarritz";
			case "NEV" -> "Nevers";
			case "NIC" -> "Nice";
			case "GRE" -> "Grenoble";
			case "OYO" -> "Oyonnax";
			case "MTB" -> "Montauban";
			case "BEZ" -> "Béziers";
			case "VAN" -> "Vannes";
			case "TOU" -> "Toulouse";
			default -> {
				if (fullName == null) {
					yield shortName;
				}
				String[] parts = fullName.split(" ");
				yield parts[parts.length - 1];
			}
		};
	}

	private static String positionSuffix(int position) {
		return position == 1 ? "re" : "e";
	}

	private static String numberWord(int value) {
		return switch (value) {
			case 0 -> "zéro";
			case 1 -> "un";
			case 2 -> "deux";
			case 3 -> "trois";
			case 4 -> "quatre";
			case 5 -> "cinq";
			case 6 -> "six";
			case 7 -> "sept";
			case 8 -> "huit";
			case 9 -> "neuf";
			case 10 -> "dix";
			default -> String.valueOf(value);
		};
	}
}
