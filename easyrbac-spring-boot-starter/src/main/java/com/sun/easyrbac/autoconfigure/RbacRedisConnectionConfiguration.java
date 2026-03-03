package com.sun.easyrbac.autoconfigure;

import com.sun.easyrbac.config.RbacProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 当项目中未配置 spring.data.redis（即没有 StringRedisTemplate）时，
 * 使用 rbac.check.redis 的 host/port/database/password 创建独立 Redis 连接，
 * 供 RBAC 登录态与角色缓存使用。若项目已配置 spring.data.redis，则直接使用应用已有的 Redis 连接。
 *
 * @author SUNRUI
 */
@Configuration
@ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(StringRedisTemplate.class)
@ConditionalOnClass(LettuceConnectionFactory.class)
public class RbacRedisConnectionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RbacRedisConnectionConfiguration.class);

    @Bean
    public LettuceConnectionFactory rbacRedisConnectionFactory(RbacProperties properties) {
        RbacProperties.Redis redis = properties.getCheck().getRedis();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost() != null ? redis.getHost() : "localhost");
        config.setPort(redis.getPort());
        config.setDatabase(redis.getDatabase());
        if (redis.getPassword() != null && !redis.getPassword().isEmpty()) {
            config.setPassword(RedisPassword.of(redis.getPassword()));
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        log.info("[EasyRBAC] Redis 连接完成，用于登录态与角色缓存：redis://{}:{}/db{}",
                config.getHostName(), config.getPort(), config.getDatabase());

        return factory;
    }

    @Bean
    public StringRedisTemplate rbacStringRedisTemplate(LettuceConnectionFactory rbacRedisConnectionFactory) {
        return new StringRedisTemplate(rbacRedisConnectionFactory);
    }
}
