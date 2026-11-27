package com.soothingtech.glean.account.facade;

import com.soothingtech.glean.account.domain.ConsentLevel;

import java.util.UUID;

/**
 * 对内授权接口（5.7.3 第 161—164 行）。
 * ingest / task / remind / view 在入口处调用，授权判断只在这里，不在各模块重复写 if。
 */
public interface ConsentFacade {

    ConsentLevel currentLevel(UUID userId);

    /** 不足直接抛 E5005（403） */
    boolean atLeast(UUID userId, ConsentLevel required);
}
