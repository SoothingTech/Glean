package com.soothingtech.glean.common;

/**
 * 错误码，严格对齐 docs/概要设计-初稿.md 5.8.1 与 5.7.3「错误码归属」。
 *
 * <p>E5xxx 段属账号与授权，已定义 E5001–E5005。新增码必须落在本段空位且永不复用，
 * 因此本次补充 E5006（验证码错误）与公共段 E9001（参数校验）两处，均为文档未覆盖、
 * 但实现必需的场景，需回填到 5.8.1 表格。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    /** 未认证 / 令牌过期 */
    public static final String UNAUTHENTICATED = "E5001";

    /** 无权限（已认证但无权访问该资源） */
    public static final String FORBIDDEN = "E5002";

    /** 版本冲突 */
    public static final String VERSION_CONFLICT = "E5003";

    /** 幂等键复用但载荷不同 */
    public static final String IDEMPOTENT_CONFLICT = "E5004";

    /** 授权级别不足 */
    public static final String CONSENT_INSUFFICIENT = "E5005";

    /** 验证码错误或已失效（本模块段空位新增，待回填 5.8.1） */
    public static final String SMS_CODE_INVALID = "E5006";

    /** 参数校验失败（公共段补充，文档 5.8.1 未定义 400 类错误码） */
    public static final String BAD_REQUEST = "E9001";
}
