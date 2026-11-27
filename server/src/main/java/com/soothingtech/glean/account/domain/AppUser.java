package com.soothingtech.glean.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 用户账号。表名用 app_user —— user 是 PostgreSQL 保留字。
 * 对应迁移脚本 db/migration/V1__account.sql。
 * created_at / updated_at 交给数据库默认值，不映射，避免 ddl-auto=validate 类型不匹配。
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "phone", nullable = false, length = 20, unique = true)
    private String phone;

    /** 授权级别，对应 JWT 的 lvl claim */
    @Column(name = "lvl", nullable = false)
    private short lvl;

    protected AppUser() {
        // JPA 要求
    }

    public AppUser(UUID id, String phone, short lvl) {
        this.id = id;
        this.phone = phone;
        this.lvl = lvl;
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public short getLvl() {
        return lvl;
    }

    public void setLvl(short lvl) {
        this.lvl = lvl;
    }
}
