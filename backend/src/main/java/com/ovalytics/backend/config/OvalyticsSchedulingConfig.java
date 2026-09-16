package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ LiveScoreProperties.class, TeamRefreshProperties.class })
public class OvalyticsSchedulingConfig {
}
