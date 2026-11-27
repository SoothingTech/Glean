package com.soothingtech.glean.account.api;

import com.soothingtech.glean.account.api.dto.AuditItemView;
import com.soothingtech.glean.account.api.dto.AuditPageView;
import com.soothingtech.glean.account.app.AuditService;
import com.soothingtech.glean.account.token.AuthPrincipal;
import com.soothingtech.glean.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 本人审计记录，游标分页（5.7.3.2 第 136 行） */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<AuditPageView> page(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit) {

        AuditService.Page page = auditService.page(principal.userId(), cursor, limit);
        return ApiResponse.ok(new AuditPageView(
                page.items().stream()
                        .map(a -> new AuditItemView(
                                a.getId().toString(),
                                a.getAction(),
                                a.getPayload(),
                                com.soothingtech.glean.common.Times.of(a.getCreatedAt())))
                        .toList(),
                page.nextCursor()));
    }
}
