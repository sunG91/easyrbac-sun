package com.sun.easyrbac.cache;

import com.sun.easyrbac.core.cache.RbacRoleCache;

import java.util.List;

/**
 * 无缓存实现：不配置 Redis 时使用，角色校验直接查库。
 *
 * @author SUNRUI
 */
public class NoOpRbacRoleCache implements RbacRoleCache {

    @Override
    public List<String> getRoles(String userId) {
        return null;
    }

    @Override
    public void putRoles(String userId, List<String> roleCodes) {
        // no-op
    }

    @Override
    public void evict(String userId) {
        // no-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
