package com.ovalytics.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ovalytics.backend.domain.Competition;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

	Optional<Competition> findByCode(String code);

	List<Competition> findAllByOrderByNameAsc();
}
