package com.soothingtech.glean.common;

/** 业务异常：携带错误码与 HTTP 状态 */
public class BizException extends RuntimeException {

    private final String code;
    private final int status;

    public BizException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
