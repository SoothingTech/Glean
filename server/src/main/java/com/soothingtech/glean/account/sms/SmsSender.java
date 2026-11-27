package com.soothingtech.glean.account.sms;

/**
 * 短信通道抽象。
 * 开发期用 DevSmsSender（固定验证码），将来接阿里云/腾讯云只换实现，
 * 不必触碰登录流程 —— 真实短信需要企业主体资质，学生阶段无法申请。
 */
public interface SmsSender {

    /**
     * 生成并向该手机号下发验证码。
     *
     * @return 实际下发的验证码，由调用方存入 Redis 做校验
     */
    String sendCode(String phone);
}
