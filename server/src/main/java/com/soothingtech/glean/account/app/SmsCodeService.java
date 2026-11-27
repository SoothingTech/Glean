package com.soothingtech.glean.account.app;

import com.soothingtech.glean.account.sms.SmsSender;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 验证码的生成、下发与校验，验证码存 Redis 并带 TTL */
@Service
public class SmsCodeService {

    private static final String PREFIX = "auth:sms:";

    private final StringRedisTemplate redis;
    private final SmsSender smsSender;
    private final Duration ttl;

    public SmsCodeService(StringRedisTemplate redis,
                          SmsSender smsSender,
                          @Value("${glean.auth.sms-code-ttl}") Duration ttl) {
        this.redis = redis;
        this.smsSender = smsSender;
        this.ttl = ttl;
    }

    public Duration sendCode(String phone) {
        String code = smsSender.sendCode(phone);
        redis.opsForValue().set(PREFIX + phone, code, ttl);
        return ttl;
    }

    /** 校验成功后立即销毁，防止重放 */
    public void verify(String phone, String code) {
        String expected = redis.opsForValue().get(PREFIX + phone);
        if (expected == null || !expected.equals(code)) {
            throw new BizException(ErrorCodes.SMS_CODE_INVALID, 400, "验证码错误或已过期");
        }
        redis.delete(PREFIX + phone);
    }
}
