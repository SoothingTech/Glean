package com.soothingtech.glean.account.event;

import com.soothingtech.glean.account.domain.ConsentLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * ConsentChanged 事件发布（5.7.3 第 181 行）。
 *
 * <p>载荷：{userId, fromLevel, toLevel, scope, changedAt}。
 * 消费端以 changedAt 作撤回水位，丢弃迟到的旧事件（第 174 行），所以 changedAt 必须取数据库
 * 写入时刻，不能取发布时刻。
 *
 * <p>account 只发布、不调用业务模块（5.2.3 硬约束）：撤回由 ingest / task / remind / view 各自订阅完成。
 *
 * <p>注意：Spring Boot 4.2 默认 JSON 库已是 Jackson 3（包名 tools.jackson），Jackson 2 的
 * com.fasterxml.jackson 不在 classpath 上。scope 结构固定为字符串/字符串数组，这里用确定性
 * 序列化生成 JSON 文本，不引入额外依赖；待 common-* 模块提供统一 JSON 组件后替换。
 */
@Component
public class ConsentEventPublisher {

    public static final String STREAM_KEY = "glean:events:consent_changed";

    private static final Logger log = LoggerFactory.getLogger(ConsentEventPublisher.class);

    private final StringRedisTemplate redis;

    public ConsentEventPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(UUID userId,
                        ConsentLevel fromLevel,
                        ConsentLevel toLevel,
                        Map<String, Object> scope,
                        OffsetDateTime changedAt) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("eventId", UUID.randomUUID().toString());
        body.put("eventType", "ConsentChanged");
        body.put("userId", userId.toString());
        body.put("fromLevel", fromLevel.name());
        body.put("toLevel", toLevel.name());
        body.put("scope", toJson(scope));
        body.put("changedAt", changedAt.toString());

        try {
            redis.opsForStream().add(StreamRecords.mapBacked(body).withStreamKey(STREAM_KEY));
        } catch (RuntimeException e) {
            // 发布失败不回滚授权变更：级别已落库，atLeast 实时查库仍能兜底（5.7.3 第 186 行）
            log.error("ConsentChanged 事件发布失败 userId={} from={} to={}", userId, fromLevel, toLevel, e);
        }
    }

    /** 确定性 JSON：Map 键排序，保证同一 scope 产出同一文本 */
    static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else if (value instanceof CharSequence) {
            quote(sb, value.toString());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : new TreeMap<>(map).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                write(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof Collection<?> collection) {
            sb.append('[');
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item);
            }
            sb.append(']');
        } else {
            quote(sb, String.valueOf(value));
        }
    }

    private static void quote(StringBuilder sb, String text) {
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
