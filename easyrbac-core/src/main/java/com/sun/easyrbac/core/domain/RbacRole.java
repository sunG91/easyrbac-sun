package com.sun.easyrbac.core.domain;

/**
 * 角色领域模型（对应角色表一行）。
 * 仅包含框架关心的字段，具体表结构由 starter 层映射。
 *
 * @author SUNRUI
 */
public class RbacRole {

    private Long id;
    private String roleCode;
    private String roleName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
