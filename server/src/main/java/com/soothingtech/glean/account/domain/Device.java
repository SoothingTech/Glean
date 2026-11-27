package com.soothingtech.glean.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 设备。主键直接用客户端上报的 deviceId，与 JWT 的 dev claim 同一取值 */
@Entity
@Table(name = "device")
public class Device {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Device() {
    }

    public Device(String id, UUID userId, String platform) {
        OffsetDateTime now = OffsetDateTime.now();
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.lastSeenAt = now;
        this.createdAt = now;
    }

    /** 登录时刷新在线时间；设备曾被下线则重新启用 */
    public void touch(String platform) {
        this.lastSeenAt = OffsetDateTime.now();
        this.revokedAt = null;
        if (platform != null && !platform.isBlank()) {
            this.platform = platform;
        }
    }

    public void revoke() {
        this.revokedAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPlatform() {
        return platform;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
