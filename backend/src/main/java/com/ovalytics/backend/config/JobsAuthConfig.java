package com.ovalytics.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ovalytics.backend.web.JobsAuthFilter;

@Configuration
@EnableConfigurationProperties(JobsAuthProperties.class)
public class JobsAuthConfig {

	@Bean
	public JobsAuthFilter jobsAuthFilter(JobsAuthProperties properties) {
		return new JobsAuthFilter(properties);
	}
}
