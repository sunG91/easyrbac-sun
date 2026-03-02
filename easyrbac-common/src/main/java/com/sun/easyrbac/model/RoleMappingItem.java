package com.sun.easyrbac.model;

import java.util.Objects;

/**
 * 角色映射项：角色编码 → 角色名称。
 * 对应配置 rbac.auto.role-mapping 中的一项。
 *
 * @author SUNRUI
 */
public class RoleMappingItem {

    /** 角色编码（如 10000），与注解中使用的 ID 一致 */
    private final String roleCode;
    /** 角色名称（如 超级管理员） */
    private final String roleName;

    public RoleMappingItem(String roleCode, String roleName) {
        this.roleCode = Objects.requireNonNull(roleCode, "roleCode");
        this.roleName = roleName != null ? roleName : "";
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }
}
