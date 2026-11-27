package com.soothingtech.glean.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    Optional<Consent> findByUserId(UUID userId);
}
