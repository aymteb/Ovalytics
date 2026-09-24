package com.ovalytics.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_items")
public class NewsItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 300)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(columnDefinition = "TEXT")
	private String body;

	@Column(length = 500)
	private String imageUrl;

	@Column(nullable = false, unique = true, length = 500)
	private String sourceUrl;

	@Column(nullable = false)
	private LocalDateTime publishedAt;

	@Column(nullable = false, length = 80)
	private String source;

	@Column(length = 20)
	private String competitionCode;

	protected NewsItem() {
	}

	public NewsItem(
			String title,
			String summary,
			String body,
			String imageUrl,
			String sourceUrl,
			LocalDateTime publishedAt,
			String source,
			String competitionCode) {
		this.title = title;
		this.summary = summary;
		this.body = body;
		this.imageUrl = imageUrl;
		this.sourceUrl = sourceUrl;
		this.publishedAt = publishedAt;
		this.source = source;
		this.competitionCode = competitionCode;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(LocalDateTime publishedAt) {
		this.publishedAt = publishedAt;
	}

	public String getSource() {
		return source;
	}

	public String getCompetitionCode() {
		return competitionCode;
	}

	public void setCompetitionCode(String competitionCode) {
		this.competitionCode = competitionCode;
	}
}
