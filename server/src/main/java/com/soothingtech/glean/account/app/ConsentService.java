package com.soothingtech.glean.account.app;

import com.soothingtech.glean.account.api.dto.ConsentUpdateResult;
import com.soothingtech.glean.account.api.dto.ConsentView;
import com.soothingtech.glean.account.domain.Consent;
import com.soothingtech.glean.account.domain.ConsentLevel;
import com.soothingtech.glean.account.domain.ConsentRepository;
import com.soothingtech.glean.account.event.ConsentEventPublisher;
import com.soothingtech.glean.common.Times;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * 授权级别与范围（概要设计 5.3.7 / 5.7.3.2）。
 *
 * <p>升级立即生效；下调触发撤回：先算撤回影响面，落库后发布 ConsentChanged，
 * 由 ingest / task / remind / view 各自执行断开、清理、注销围栏、失效缓存。
 */
@Service
public class ConsentService {

    private final ConsentRepository consents;
    private final AuditService audit;
    private final IdempotencyService idempotency;
    private final ConsentEventPublisher events;

    public ConsentService(ConsentRepository consents,
                          AuditService audit,
                          IdempotencyService idempotency,
                          ConsentEventPublisher events) {
        this.consents = consents;
        this.audit = audit;
        this.idempotency = idempotency;
        this.events = events;
    }

    /** 读取当前授权；从未设置过则按默认 L1 落库（概要设计 5.3.7「默认 L1」） */
    @Transactional
    public Consent current(UUID userId) {
        return consents.findByUserId(userId).orElseGet(() ->
                consents.save(new Consent(userId, ConsentLevel.DEFAULT, Map.of())));
    }

    public ConsentView view(UUID userId) {
        Consent consent = current(userId);
        return new ConsentView(
                consent.getLevel().name(),
                consent.getScope(),
                Times.of(consent.getGrantedAt()),
                Times.of(consent.getRevokedAt()),
                Times.of(consent.getUpdatedAt()));
    }

    /** 调整级别与范围。幂等键相同且载荷一致视为重放，结果一致 */
    @Transactional
    public ConsentUpdateResult update(UUID userId,
                                      ConsentLevel target,
                                      Map<String, Object> scope,
                                      String requestKey) {
        Map<String, Object> normalized = scope == null ? Map.of() : new LinkedHashMap<>(scope);
        idempotency.check(userId, requestKey, fingerprint(target, normalized));

        Consent consent = current(userId);
        ConsentLevel from = consent.getLevel();
        boolean lowering = target.ordinal() < from.ordinal();

        ConsentUpdateResult.Revoked revoked = lowering
                ? countRevoked(consent.getScope(), normalized)
                : ConsentUpdateResult.Revoked.NONE;

        consent.apply(target, normalized, lowering);
        consents.save(consent);

        OffsetDateTime changedAt = consent.getUpdatedAt();
        events.publish(userId, from, target, consent.getScope(), changedAt);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("fromLevel", from.name());
        auditPayload.put("toLevel", target.name());
        auditPayload.put("lowering", lowering);
        auditPayload.put("revokedGroups", revoked.groups());
        auditPayload.put("revokedApps", revoked.apps());
        audit.record(userId, AuditService.ACTION_CONSENT_UPDATE, auditPayload);

        return new ConsentUpdateResult(target.name(), revoked, Times.of(changedAt));
    }

    /** 撤回影响面：旧范围里有、新范围里没有的群与应用数量 */
    private ConsentUpdateResult.Revoked countRevoked(Map<String, Object> before, Map<String, Object> after) {
        return new ConsentUpdateResult.Revoked(
                removed(before.get("qqGroups"), after.get("qqGroups")),
                removed(before.get("notifyApps"), after.get("notifyApps")));
    }

    private int removed(Object before, Object after) {
        Set<String> old = asSet(before);
        old.removeAll(asSet(after));
        return old.size();
    }

    private Set<String> asSet(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        }
        return result;
    }

    /**
     * 载荷指纹：级别 + 键排序后的 scope 文本，保证同载荷得到同指纹。
     * 不使用 JSON 库，避免与 Boot 4 的 Jackson 3 绑死（见 ConsentEventPublisher 说明）。
     */
    private String fingerprint(ConsentLevel level, Map<String, Object> scope) {
        StringBuilder sb = new StringBuilder(level.name()).append('|');
        for (String key : new TreeSet<>(scope.keySet())) {
            Object value = scope.get(key);
            sb.append(key).append('=');
            if (value instanceof Collection<?> collection) {
                List<String> items = collection.stream()
                        .map(String::valueOf)
                        .sorted()
                        .toList();
                sb.append(String.join(",", items));
            } else {
                sb.append(value);
            }
            sb.append(';');
        }
        return sb.toString();
    }
}
