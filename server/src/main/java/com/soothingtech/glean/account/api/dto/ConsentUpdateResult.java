package com.soothingtech.glean.account.api.dto;

import java.time.OffsetDateTime;

/**
 * PUT /consent 的响应数据，与原文档示例同形：
 * {"level":"L3","revoked":{"groups":2,"apps":3},"effectiveAt":"..."}
 */
public record ConsentUpdateResult(
        String level,
        Revoked revoked,
        OffsetDateTime effectiveAt) {

    /** 撤回影响面：供前端提示「将断开 N 个群、停止 M 个应用的通知」 */
    public record Revoked(int groups, int apps) {

        public static final Revoked NONE = new Revoked(0, 0);
    }
}
