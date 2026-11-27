package com.soothingtech.glean.common;

/**
 * 成功响应包封：{"code":"0","data":...}。
 * 依据 5.7.3 PUT /consent 的响应示例，客户端按 HTTP 状态 + code 两维判定（5.7.3 第 47 行）。
 */
public record ApiResponse<T>(String code, T data) {

    public static final String OK = "0";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK, data);
    }
}
