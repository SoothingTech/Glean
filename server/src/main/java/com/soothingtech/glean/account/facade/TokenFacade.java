package com.soothingtech.glean.account.facade;

import com.soothingtech.glean.account.token.AuthPrincipal;

import java.util.UUID;

/** 对内令牌接口（5.7.3 第 166—169 行） */
public interface TokenFacade {

    /** 校验 Bearer 令牌；失败抛 E5001 */
    AuthPrincipal verify(String bearer);

    /** 为指定用户与设备签发访问令牌 */
    String issue(UUID userId, String deviceId);
}
