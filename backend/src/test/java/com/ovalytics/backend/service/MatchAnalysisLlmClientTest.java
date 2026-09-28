package com.ovalytics.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ovalytics.backend.config.AnalysisProperties;

class MatchAnalysisLlmClientTest {

	@Test
	void hasApiKeyDefaultsToFalse() {
		assertThat(new AnalysisProperties().hasApiKey()).isFalse();
	}
}
