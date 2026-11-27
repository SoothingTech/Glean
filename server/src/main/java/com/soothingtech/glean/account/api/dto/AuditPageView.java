package com.soothingtech.glean.account.api.dto;

import java.util.List;

/**
 * 审计游标分页。
 * nextCursor 为空表示到底；下一页把 nextCursor 原样回传即可。
 */
public record AuditPageView(List<AuditItemView> items, String nextCursor) {
}
