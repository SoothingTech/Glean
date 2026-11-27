package com.soothingtech.glean.account.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录请求：手机号 + 验证码（文档 5.7.3.2 第 129 行）。
 * deviceId 对应 JWT 的 dev claim；platform 用于设备列表展示（Android / Web / iOS）。
 */
public record LoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Size(min = 4, max = 8, message = "验证码长度不正确")
        String code,

        String deviceId,

        String platform) {
}
