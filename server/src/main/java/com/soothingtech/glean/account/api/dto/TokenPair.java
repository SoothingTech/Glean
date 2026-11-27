package com.soothingtech.glean.account.api.dto;

/** 登录/刷新的返回体：accessToken / refreshToken / expiresIn（文档 5.7.3.2 第 129 行） */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
}
