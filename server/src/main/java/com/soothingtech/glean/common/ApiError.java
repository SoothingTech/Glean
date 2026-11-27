package com.soothingtech.glean.common;

/** 统一错误响应体：客户端只认 code（文档第 47 行） */
public record ApiError(String code, String message) {
}
