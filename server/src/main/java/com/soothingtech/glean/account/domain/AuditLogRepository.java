package com.soothingtech.glean.account.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    /**
     * 游标分页：cursor 为上一页最后一条的 created_at。
     * created_at 相同的情况下由 id 兜底排序，避免抖动。
     */
    List<AuditLog> findByUserIdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
            UUID userId, OffsetDateTime cursor, Pageable pageable);
}
