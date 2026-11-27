package com.soothingtech.glean.account.facade.impl;

import com.soothingtech.glean.account.app.ConsentService;
import com.soothingtech.glean.account.domain.ConsentLevel;
import com.soothingtech.glean.account.facade.ConsentFacade;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConsentFacadeImpl implements ConsentFacade {

    private final ConsentService consentService;

    public ConsentFacadeImpl(ConsentService consentService) {
        this.consentService = consentService;
    }

    /** 实时查库：JWT 里的 lvl 不追溯，必须以此处为准（5.7.3 第 186 行） */
    @Override
    public ConsentLevel currentLevel(UUID userId) {
        return consentService.current(userId).getLevel();
    }

    @Override
    public boolean atLeast(UUID userId, ConsentLevel required) {
        if (!currentLevel(userId).covers(required)) {
            throw new BizException(ErrorCodes.CONSENT_INSUFFICIENT, 403, "授权级别不足，需开启对应授权");
        }
        return true;
    }
}
