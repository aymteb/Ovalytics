package com.ovalytics.backend.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ovalytics.backend.config.JobsAuthProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JobsAuthFilter extends OncePerRequestFilter {

	private final JobsAuthProperties properties;

	public JobsAuthFilter(JobsAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!properties.isEnabled()) {
			return true;
		}
		String path = request.getRequestURI();
		return path == null || !path.startsWith("/api/jobs");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String expected = properties.getKey() == null ? "" : properties.getKey().trim();
		if (expected.isEmpty()) {
			response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("Jobs auth non configuree");
			return;
		}

		String provided = request.getHeader(properties.getHeader());
		if (provided == null || !constantTimeEquals(expected, provided.trim())) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("Unauthorized");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private static boolean constantTimeEquals(String left, String right) {
		byte[] a = left.getBytes(StandardCharsets.UTF_8);
		byte[] b = right.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(a, b);
	}
}
