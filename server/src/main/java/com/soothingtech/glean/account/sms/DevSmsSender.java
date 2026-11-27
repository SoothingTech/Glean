package com.soothingtech.glean.account.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 开发环境短信实现：验证码固定为配置值（默认 123456），只打日志不真发。
 * prod profile 下不生效，避免误上线。
 */
@Component
@Profile("!prod")
public class DevSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(DevSmsSender.class);

    private final String fixedCode;

    public DevSmsSender(@Value("${glean.auth.dev-fixed-code}") String fixedCode) {
        this.fixedCode = fixedCode;
    }

    @Override
    public String sendCode(String phone) {
        log.info("[DEV-SMS] 手机号 {} 的验证码为 {}（未接入真实短信通道）", phone, fixedCode);
        return fixedCode;
    }
}
