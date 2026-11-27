package com.soothingtech.glean.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 授权级别与范围，对应表 consent（概要设计 5.6 数据模型） */
@Entity
@Table(name = "consent")
public class Consent {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 2)
    private ConsentLevel level;

    /** 与 PUT /consent 请求体 scope 同构：notifyApps / qqGroups / quietHours */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope", nullable = false)
    private Map<String, Object> scope = new LinkedHashMap<>();

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Consent() {
    }

    public Consent(UUID userId, ConsentLevel level, Map<String, Object> scope) {
        OffsetDateTime now = OffsetDateTime.now();
        this.userId = userId;
        this.level = level;
        this.scope = scope == null ? new LinkedHashMap<>() : new LinkedHashMap<>(scope);
        this.grantedAt = now;
        this.updatedAt = now;
    }

    /**
     * 调整级别与范围。
     *
     * @param lowering 是否为下调（触发撤回：记 revoked_at）
     */
    public void apply(ConsentLevel target, Map<String, Object> newScope, boolean lowering) {
        OffsetDateTime now = OffsetDateTime.now();
        this.level = target;
        this.scope = newScope == null ? new LinkedHashMap<>() : new LinkedHashMap<>(newScope);
        this.updatedAt = now;
        if (lowering) {
            this.revokedAt = now;
        } else {
            this.grantedAt = now;
            this.revokedAt = null;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public ConsentLevel getLevel() {
        return level;
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
