package com.sun.easyrbac.service;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.model.RbacTokenResult;
import com.sun.easyrbac.core.token.RbacTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * 登录后签发 Token 的封装服务。业务只需在「登录校验通过」后传入 userId，即可拿到与当前校验配置一致的 Token，
 * 无需自己实现 HMAC/JWT 或关心过期时间、请求头前缀等。
 * <p>
 * - type=internal：由框架生成 internal Token（payload 含 userId:expireAt 毫秒时间戳，HMAC 签名），直接返回。
 * - type=jwt：本框架不实现 JWT 签发，{@link #issueToken(Object)} 返回 null，业务需用自有 JWT 库签发。
 *
 * 若项目启用了 Redis（存在 StringRedisTemplate），可通过 {@link #issueTokenAndCache(Object)} 在签发 Token 的同时
 * 将登录态写入 Redis（key 由 rbac.check.redis.key-prefix 与 key-by 决定，value 默认为 userId）。
 *
 * @author SUNRUI
 */
public class RbacTokenIssuerService {

    private static final Logger log = LoggerFactory.getLogger(RbacTokenIssuerService.class);

    private final RbacTokenValidator tokenValidator;
    private final RbacProperties properties;
    @Nullable
    private final StringRedisTemplate redisTemplate;

    public RbacTokenIssuerService(RbacTokenValidator tokenValidator, RbacProperties properties) {
        this(tokenValidator, properties, null);
    }

    public RbacTokenIssuerService(RbacTokenValidator tokenValidator,
                                  RbacProperties properties,
                                  @Nullable StringRedisTemplate redisTemplate) {
        this.tokenValidator = tokenValidator;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 为指定用户签发 Token，与当前 rbac.check 配置一致，可直接放入登录响应返回前端。
     * 用户标识类型可在 yaml 中配置 rbac.check.internal.user-identifier-type（string/long/integer），
     * 此处传入任意类型（String、Long、Integer 等）均可，框架会转为字符串存储。
     *
     * @param userId 用户唯一标识，可为 String、Long、Integer 等（internal 时写入 payload，jwt 时由业务在 JWT 的 sub 中填写）
     * @return internal 类型时返回 token + type + expireSeconds + userId 字符串；jwt 类型时返回 null（需业务自行签发 JWT）
     */
    @Nullable
    public RbacTokenResult issueToken(Object userId) {
        if (userId == null) {
            return null;
        }
        String userIdStr = String.valueOf(userId);
        if (userIdStr.isEmpty()) {
            return null;
        }
        String type = properties.getCheck().getType();
        if ("internal".equalsIgnoreCase(type)) {
            String token = tokenValidator.generate(userId);
            if (token == null) return null;
            String prefix = properties.getCheck().getInternal().getPrefix();
            long expireSeconds = properties.getCheck().getInternal().getExpireSeconds() > 0
                    ? properties.getCheck().getInternal().getExpireSeconds()
                    : 7200;
            return new RbacTokenResult(token, prefix != null ? prefix : "Bearer", expireSeconds, userIdStr);
        }
        // jwt：框架不实现签发，返回 null
        return null;
    }

    /**
     * 为指定用户签发 Token，并在启用 Redis 时自动将登录态写入 Redis。
     * <p>
     * - key 形如：key-prefix + userId（默认）或 key-prefix + token（当 key-by=token 时）
     * - value 默认为 userId
     * - 过期时间取 rbac.check.redis.expire-time，未配置时默认 7200 秒
     *
     * @param userId 用户唯一标识
     * @return internal 类型时返回 token 信息；jwt 类型或其他情况返回 null
     */
    @Nullable
    public RbacTokenResult issueTokenAndCache(Object userId) {
        RbacTokenResult result = issueToken(userId);
        if (result == null) {
            return null;
        }
        cacheLoginState(result.getUserId(), result.getToken());
        return result;
    }

    private void cacheLoginState(String userId, String token) {
        if (redisTemplate == null) {
            // 未启用 Redis 时忽略
            return;
        }
        try {
            RbacProperties.Redis redis = properties.getCheck().getRedis();
            if (redis == null) {
                return;
            }
            String prefix = redis.getKeyPrefix() != null ? redis.getKeyPrefix() : "rbac:login:";
            String keyBy = redis.getKeyBy() != null ? redis.getKeyBy() : "user_id";
            String key;
            if ("token".equalsIgnoreCase(keyBy)) {
                key = prefix + token;
            } else {
                // 默认按 userId 作为 key
                key = prefix + userId;
            }
            long expireSeconds = redis.getExpireTime() > 0 ? redis.getExpireTime() : 7200;
            redisTemplate.opsForValue().set(key, userId, Duration.ofSeconds(expireSeconds));
        } catch (Exception e) {
            log.warn("[EasyRBAC] Redis cache login state error: {}", e.getMessage());
        }
    }
}
