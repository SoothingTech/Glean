package com.soothingtech.glean.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findByUserIdOrderByLastSeenAtDesc(UUID userId);

    Optional<Device> findByIdAndUserId(String id, UUID userId);
}
