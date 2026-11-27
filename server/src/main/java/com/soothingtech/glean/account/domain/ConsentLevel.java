package com.soothingtech.glean.account.domain;

import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;

/**
 * 授权级别，累进式（概要设计 5.3.7）：L3 必然含 L2 能力。
 * 代码里只允许用 {@link #covers(ConsentLevel)} 比较，不得出现「L4 才可 L2 操作」这类散落判断。
 */
public enum ConsentLevel {

    /** 关闭：不采集任何来源 */
    L0,
    /** 手动投递：转发、粘贴、截图、语音 */
    L1,
    /** 机器人会话：L1 + QQ/微信群与单聊 */
    L2,
    /** 端侧通知感知：L2 + 白名单应用通知 */
    L3,
    /** 附件与语音：L3 + 截图/语音附件加密存储 */
    L4;

    /** 新用户默认级别（概要设计 5.3.7：默认 L1） */
    public static final ConsentLevel DEFAULT = L1;

    /** 本级别是否已包含 required 的能力（累进语义的唯一实现处） */
    public boolean covers(ConsentLevel required) {
        return ordinal() >= required.ordinal();
    }

    public static ConsentLevel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCodes.BAD_REQUEST, 400, "授权级别只能是 L0—L4");
        }
    }
}
