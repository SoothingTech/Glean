package com.soothingtech.glean.account.api.dto;

/** 当前登录者信息，用于验证令牌有效 */
public record MeResponse(String userId, String deviceId, int lvl) {
}
