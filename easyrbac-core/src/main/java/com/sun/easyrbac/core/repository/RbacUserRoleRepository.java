package com.sun.easyrbac.core.repository;

import java.util.List;

/**
 * 用户-角色关联表仓储接口。
 * 用户-角色由业务维护，不参与启动时自动同步。
 *
 * @author SUNRUI
 */
public interface RbacUserRoleRepository {

    /**
     * 为用户添加角色
     */
    void addRole(String userId, Long roleId);

    /**
     * 移除用户某角色
     */
    void removeRole(String userId, Long roleId);

    /**
     * 查询用户拥有的角色 ID 列表
     */
    List<Long> findRoleIdsByUserId(String userId);

    /**
     * 判断用户是否拥有某角色
     */
    boolean hasRole(String userId, Long roleId);
}
