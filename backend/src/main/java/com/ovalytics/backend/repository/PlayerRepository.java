package com.ovalytics.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ovalytics.backend.domain.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	long count();
}
