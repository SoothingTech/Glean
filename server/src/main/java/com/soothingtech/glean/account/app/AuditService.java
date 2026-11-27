package com.soothingtech.glean.account.app;

import com.soothingtech.glean.account.domain.AuditLog;
import com.soothingtech.glean.account.domain.AuditLogRepository;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计。概要设计 5.3.7：撤回「全过程写审计」，因此授权变更、登录、登出、设备下线都落这里。
 */
@Service
public class AuditService {

    public static final String ACTION_LOGIN = "auth.login";
    public static final String ACTION_LOGOUT = "auth.logout";
    public static final String ACTION_CONSENT_UPDATE = "consent.update";
    public static final String ACTION_DEVICE_REVOKE = "device.revoke";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID userId, String action, Map<String, Object> payload) {
        repository.save(new AuditLog(userId, action, payload));
    }

    public Page page(UUID userId, String cursor, Integer limit) {
        int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        // 多取一条用来判断是否还有下一页
        PageRequest request = PageRequest.of(0, size + 1);

        List<AuditLog> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, request);
        } else {
            OffsetDateTime parsed;
            try {
                parsed = OffsetDateTime.parse(cursor);
            } catch (DateTimeParseException e) {
                throw new BizException(ErrorCodes.BAD_REQUEST, 400, "cursor 不是合法的 ISO-8601 时间");
            }
            rows = repository.findByUserIdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(userId, parsed, request);
        }

        boolean hasMore = rows.size() > size;
        List<AuditLog> page = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? page.get(page.size() - 1).getCreatedAt().toString() : null;
        return new Page(page, next);
    }

    public record Page(List<AuditLog> items, String nextCursor) {
    }
}
