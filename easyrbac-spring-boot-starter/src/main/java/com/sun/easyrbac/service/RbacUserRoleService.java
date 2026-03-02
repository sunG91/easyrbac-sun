package com.sun.easyrbac.service;

import com.sun.easyrbac.core.cache.RbacRoleCache;
import com.sun.easyrbac.core.domain.RbacRole;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import com.sun.easyrbac.core.repository.RbacUserRoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务层操作用户与角色：添加/移除角色、查询用户角色、判断是否拥有某角色或权限。
 * 无 Redis 时直接查库；有 Redis 时走缓存策略（getRoles/hasRole 先读缓存，addRole/removeRole 后失效对应用户缓存）。
 *
 * @author SUNRUI
 */
public class RbacUserRoleService {

    private final RbacRoleRepository roleRepository;
    private final RbacUserRoleRepository userRoleRepository;
    private final RbacRoleApiRepository roleApiRepository;
    private final RbacApiRepository apiRepository;
    private final RbacRoleCache roleCache;

    public RbacUserRoleService(RbacRoleRepository roleRepository,
                               RbacUserRoleRepository userRoleRepository,
                               RbacRoleApiRepository roleApiRepository,
                               RbacApiRepository apiRepository,
                               RbacRoleCache roleCache) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleApiRepository = roleApiRepository;
        this.apiRepository = apiRepository;
        this.roleCache = roleCache != null ? roleCache : noOpCache();
    }

    private static RbacRoleCache noOpCache() {
        return new com.sun.easyrbac.cache.NoOpRbacRoleCache();
    }

    /** 为指定用户绑定单个角色（按角色编码）；若启用 Redis 缓存则会失效该用户角色缓存 */
    public void addRole(String userId, String roleCode) {
        roleRepository.findByRoleCode(roleCode).ifPresent(role -> userRoleRepository.addRole(userId, role.getId()));
        if (roleCache.isEnabled()) {
            roleCache.evict(userId);
        }
    }

    /** 批量绑定角色，减少数据库往返；若启用缓存则失效该用户缓存 */
    public void addRoles(String userId, List<String> roleCodes) {
        if (roleCodes == null) return;
        for (String code : roleCodes) {
            roleRepository.findByRoleCode(code).ifPresent(role -> userRoleRepository.addRole(userId, role.getId()));
        }
        if (roleCache.isEnabled()) {
            roleCache.evict(userId);
        }
    }

    /** 解除用户与某角色的绑定；若启用缓存则失效该用户缓存 */
    public void removeRole(String userId, String roleCode) {
        roleRepository.findByRoleCode(roleCode).ifPresent(role -> userRoleRepository.removeRole(userId, role.getId()));
        if (roleCache.isEnabled()) {
            roleCache.evict(userId);
        }
    }

    /** 批量解除角色；若启用缓存则失效该用户缓存 */
    public void removeRoles(String userId, List<String> roleCodes) {
        if (roleCodes == null) return;
        for (String code : roleCodes) {
            removeRole(userId, code);
        }
    }

    /** 返回该用户当前绑定的角色列表（角色编码）。有 Redis 时先读缓存，未命中再查库并回填缓存 */
    public List<String> getRoles(String userId) {
        if (roleCache.isEnabled()) {
            List<String> cached = roleCache.getRoles(userId);
            if (cached != null) return cached;
        }
        List<String> roles = getRolesFromDb(userId);
        if (roleCache.isEnabled() && !roles.isEmpty()) {
            roleCache.putRoles(userId, roles);
        }
        return roles;
    }

    private List<String> getRolesFromDb(String userId) {
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return new ArrayList<>();
        List<RbacRole> allRoles = roleRepository.findAll();
        return roleIds.stream()
                .map(id -> allRoles.stream().filter(r -> r.getId() != null && r.getId().equals(id)).findFirst().map(RbacRole::getRoleCode).orElse(null))
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 判断用户是否拥有某角色；有 Redis 时通过 getRoles（走缓存）判断 */
    public boolean hasRole(String userId, String roleCode) {
        return getRoles(userId).contains(roleCode);
    }

    /** 判断用户是否拥有某权限（按权限 path 或权限 ID 对应的接口） */
    public boolean hasPermission(String userId, String permissionIdOrPath) {
        List<Long> userRoleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (userRoleIds.isEmpty()) return false;
        var apiOpt = apiRepository.findByPath(permissionIdOrPath);
        if (apiOpt.isEmpty()) return false;
        Long apiId = apiOpt.get().getId();
        for (Long roleId : userRoleIds) {
            if (roleApiRepository.findApiIdsByRoleId(roleId).contains(apiId)) {
                return true;
            }
        }
        return false;
    }
}
