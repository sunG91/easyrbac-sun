package com.sun.easyrbac.core.cache;

import java.util.List;

/**
 * 用户角色缓存接口。
 * 无 Redis 时使用 NoOp 实现（直接查库）；有 Redis 时使用 Redis 实现并配置 TTL。
 *
 * @author SUNRUI
 */
public interface RbacRoleCache {

    /**
     * 获取用户角色列表，未命中返回 null（由调用方查库后可调用 putRoles 回填）。
     */
    List<String> getRoles(String userId);

    /**
     * 将用户角色列表写入缓存。
     */
    void putRoles(String userId, List<String> roleCodes);

    /**
     * 移除某用户的缓存（如 addRole/removeRole 后需失效）。
     */
    void evict(String userId);

    /**
     * 是否启用缓存（Redis 已配置且可用时为 true）。
     */
    boolean isEnabled();
}
