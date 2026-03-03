package com.sun.easyrbac.cache;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.cache.RbacRoleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于 Redis 的用户角色缓存。
 * 配置 rbac.check.redis 后启用，校验时优先读缓存，未命中再查库并回填；addRole/removeRole 后失效对应用户缓存。
 *
 * @author SUNRUI
 */
public class RedisRbacRoleCache implements RbacRoleCache {

    private static final Logger log = LoggerFactory.getLogger(RedisRbacRoleCache.class);
    private static final String ROLE_LIST_SEPARATOR = ",";

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final long expireSeconds;

    /** 角色缓存 key 前缀，与登录态（rbac:login:）区分，不共用 check.redis.key-prefix */
    private static final String ROLE_KEY_PREFIX = "rbac:roles:";

    public RedisRbacRoleCache(StringRedisTemplate redisTemplate, RbacProperties properties) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = ROLE_KEY_PREFIX;
        RbacProperties.Redis redis = properties.getCheck().getRedis();
        long ttl = properties.getCache() != null ? properties.getCache().getTtl() : 300;
        this.expireSeconds = redis.getExpireTime() > 0 ? redis.getExpireTime() : ttl;
        log.info("[EasyRBAC] Redis 角色缓存已启用，key 前缀={}, TTL={}s", this.keyPrefix, this.expireSeconds);
    }

    @Override
    public List<String> getRoles(String userId) {
        if (userId == null || userId.isEmpty()) return null;
        String key = keyPrefix + userId;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isEmpty()) return null;
            return Stream.of(value.split(ROLE_LIST_SEPARATOR)).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("[EasyRBAC] Redis get roles miss or error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void putRoles(String userId, List<String> roleCodes) {
        if (userId == null || userId.isEmpty()) return;
        String key = keyPrefix + userId;
        try {
            String value = roleCodes != null ? String.join(ROLE_LIST_SEPARATOR, roleCodes) : "";
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
        } catch (Exception e) {
            log.warn("[EasyRBAC] Redis put roles error: {}", e.getMessage());
        }
    }

    @Override
    public void evict(String userId) {
        if (userId == null || userId.isEmpty()) return;
        try {
            redisTemplate.delete(keyPrefix + userId);
        } catch (Exception e) {
            log.debug("[EasyRBAC] Redis evict error: {}", e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
