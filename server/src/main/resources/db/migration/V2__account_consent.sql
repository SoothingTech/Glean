-- V2 账号与授权：授权级别、设备、审计
-- 依据 docs/概要设计-初稿.md 5.3.7（L0—L4 累进式，默认 L1、撤回即时生效）
-- 与 docs/5.7.3 5.7.3.2 接口表；consent 字段沿用 5.6 数据模型（level、scope jsonb、granted_at、revoked_at）

CREATE TABLE consent (
    user_id    uuid PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    -- L0 关闭 / L1 手动投递 / L2 机器人会话 / L3 端侧通知感知 / L4 附件与语音
    level      varchar(2)  NOT NULL DEFAULT 'L1',
    -- 与接口 PUT /consent 的 scope 同构：notifyApps / qqGroups / quietHours
    scope      jsonb       NOT NULL DEFAULT '{}'::jsonb,
    granted_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_consent_level CHECK (level IN ('L0', 'L1', 'L2', 'L3', 'L4'))
);

COMMENT ON TABLE consent IS '授权级别与范围；累进式，L3 必然含 L2 能力';

CREATE TABLE device (
    -- 直接采用客户端上报的 deviceId，与 JWT 的 dev claim 保持同一取值
    id           varchar(64) PRIMARY KEY,
    user_id      uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    platform     varchar(32),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    revoked_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_device_user_time ON device (user_id, last_seen_at DESC);

COMMENT ON TABLE device IS '设备列表；DELETE /devices/{id} 远程下线后 revoked_at 置位';

CREATE TABLE audit_log (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    action     varchar(48) NOT NULL,
    payload    jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 游标分页按 (created_at, id) 倒序，索引与排序一致
CREATE INDEX idx_audit_user_cursor ON audit_log (user_id, created_at DESC, id DESC);

COMMENT ON TABLE audit_log IS '本人审计记录；授权变更、登录、登出、设备下线均写审计';
