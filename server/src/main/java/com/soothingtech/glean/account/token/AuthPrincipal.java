package com.soothingtech.glean.account.token;

import java.util.UUID;

/** 认证主体，来自 JWT 的 sub / dev / lvl 三个 claim */
public record AuthPrincipal(UUID userId, String deviceId, int lvl) {
}
