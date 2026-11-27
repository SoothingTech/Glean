package com.soothingtech.glean.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 时间归一化。
 *
 * <p>PostgreSQL 的 timestamptz 读回时偏移量由驱动决定，同一接口里可能出现
 * 2026-11-27T06:57:00Z 与 2026-11-27T14:57:00+08:00 两种写法。二者是同一时刻，
 * 但对外契约应当统一——统一按 Asia/Shanghai 输出，与设计文档示例
 * 「2026-11-18T18:00:03+08:00」一致。
 */
public final class Times {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private Times() {
    }

    public static OffsetDateTime of(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZONE).toOffsetDateTime();
    }
}
