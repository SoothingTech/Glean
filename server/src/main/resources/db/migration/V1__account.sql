-- V1 账号与授权：最小表结构
-- 对应 docs/5.7.3-六大模块接口设计.md 5.7.3.2 账号与授权
-- 注意：表名用 app_user 而非 user —— user 是 PostgreSQL 保留字

CREATE TABLE app_user (
    id         uuid PRIMARY KEY,
    phone      varchar(20) NOT NULL,
    -- lvl 对应 JWT 中的 lvl claim（授权级别），文档 5.7.3.2 第 186 行
    lvl        smallint    NOT NULL DEFAULT 10,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_phone UNIQUE (phone)
);

COMMENT ON TABLE app_user IS '用户账号';
COMMENT ON COLUMN app_user.lvl IS '授权级别，对应 JWT claim lvl';
