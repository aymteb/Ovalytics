package com.ovalytics.backend.service.flashscore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ovalytics.backend.domain.MatchEventType;

class FlashscoreSummaryParserTest {

	@Test
	void parsesTriesAndPenalties() {
		String raw = "AC÷1st Half¬IG÷19¬IH÷17¬~"
				+ "III÷tSVLSaTs¬IA÷2¬IB÷9'¬IE÷206¬IF÷Martins L.¬IU÷¬ICT÷¬IK÷Try¬IM÷refDvkIs¬~"
				+ "III÷dr6BvAjn¬IA÷2¬IB÷9'¬IE÷207¬IF÷Richardis L.¬IU÷¬ICT÷¬IK÷Conversion Goal¬IM÷KWPUqgK0¬~"
				+ "AC÷2nd Half¬IG÷14¬IH÷22¬~"
				+ "III÷x48pA8Cc¬IA÷1¬IB÷31'¬IE÷208¬IF÷Alary T.¬IU÷¬ICT÷¬IK÷Penalty Goal¬IM÷fHAsvoJ1¬~"
				+ "A1÷¬~";

		List<FlashscoreSummaryParser.SummaryEvent> events = FlashscoreSummaryParser.parse(raw);

		assertThat(events).hasSize(3);
		assertThat(events.get(0).eventType()).isEqualTo(MatchEventType.TRY);
		assertThat(events.get(0).teamSide()).isEqualTo("AWAY");
		assertThat(events.get(0).playerName()).isEqualTo("Martins L.");
		assertThat(events.get(0).minuteLabel()).isEqualTo("9'");
		assertThat(events.get(0).periodLabel()).isEqualTo("1st Half");
		assertThat(events.get(1).eventType()).isEqualTo(MatchEventType.CONVERSION);
		assertThat(events.get(2).eventType()).isEqualTo(MatchEventType.PENALTY);
		assertThat(events.get(2).teamSide()).isEqualTo("HOME");
		assertThat(events.get(2).periodLabel()).isEqualTo("2nd Half");
	}

	@Test
	void parsesYellowAndRedCards() {
		String raw = "AC÷1st Half¬IG÷17¬IH÷6¬~"
				+ "III÷QJkjob7l¬IA÷1¬IB÷31'¬IE÷1¬IF÷Medrano S.¬IU÷¬ICT÷¬IK÷Yellow Card¬IM÷abc¬~"
				+ "AC÷2nd Half¬IG÷14¬IH÷22¬~"
				+ "III÷C2q7fcBk¬IA÷2¬IB÷67'¬IE÷2¬IF÷Tomkinson P.¬IU÷¬ICT÷¬IK÷Red Card¬IM÷def¬~"
				+ "A1÷¬~";

		List<FlashscoreSummaryParser.SummaryEvent> events = FlashscoreSummaryParser.parse(raw);

		assertThat(events).hasSize(2);
		assertThat(events.get(0).eventType()).isEqualTo(MatchEventType.YELLOW);
		assertThat(events.get(0).playerName()).isEqualTo("Medrano S.");
		assertThat(events.get(0).teamSide()).isEqualTo("HOME");
		assertThat(events.get(1).eventType()).isEqualTo(MatchEventType.RED);
		assertThat(events.get(1).playerName()).isEqualTo("Tomkinson P.");
		assertThat(events.get(1).teamSide()).isEqualTo("AWAY");
	}
}
