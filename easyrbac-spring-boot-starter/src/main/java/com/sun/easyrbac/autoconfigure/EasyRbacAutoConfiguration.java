package com.sun.easyrbac.autoconfigure;

import com.sun.easyrbac.cache.NoOpRbacRoleCache;
import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.cache.RbacRoleCache;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import com.sun.easyrbac.core.repository.RbacUserRoleRepository;
import com.sun.easyrbac.core.token.RbacTokenValidator;
import com.sun.easyrbac.ext.RbacExcludePathCustomizer;
import com.sun.easyrbac.filter.RbacCheckFilter;
import com.sun.easyrbac.service.RbacSyncService;
import com.sun.easyrbac.service.RbacUserRoleService;
import com.sun.easyrbac.service.RbacConfigAdminService;
import com.sun.easyrbac.support.*;
import com.sun.easyrbac.interceptor.RbacCheckInterceptor;
import com.sun.easyrbac.token.InternalTokenValidator;
import com.sun.easyrbac.token.JwtTokenValidator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EasyRBAC 1.0.0 自动配置。
 *
 * @author SUNRUI
 */
@Configuration
@ConditionalOnClass(JdbcTemplate.class)
@EnableConfigurationProperties(RbacProperties.class)
public class EasyRbacAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacColumnResolver rbacColumnResolver(RbacProperties properties) {
        return new RbacColumnResolver(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacRoleRepository rbacRoleRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        return new JdbcRbacRoleRepository(jdbcTemplate, properties, columns);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacApiRepository rbacApiRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        return new JdbcRbacApiRepository(jdbcTemplate, properties, columns);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacRoleApiRepository rbacRoleApiRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        return new JdbcRbacRoleApiRepository(jdbcTemplate, properties, columns);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacUserRoleRepository rbacUserRoleRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        return new JdbcRbacUserRoleRepository(jdbcTemplate, properties, columns);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacSchemaInitializer rbacSchemaInitializer(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        return new RbacSchemaInitializer(jdbcTemplate, properties, columns);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner rbacStartupRunner(RbacSchemaInitializer schemaInitializer,
                                               RbacSyncService syncService,
                                               RbacProperties properties) {
        return args -> {
            schemaInitializer.createTablesIfNeeded();
            if (properties.getAuto().isSyncAsync()) {
                new Thread(syncService::sync, "rbac-sync").start();
            } else {
                syncService.sync();
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacSyncService rbacSyncService(RbacProperties properties,
                                           RbacRoleRepository roleRepository,
                                           RbacApiRepository apiRepository,
                                           RbacRoleApiRepository roleApiRepository,
                                           ApplicationContext applicationContext) {
        return new RbacSyncService(properties, roleRepository, apiRepository, roleApiRepository, applicationContext);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacConfigAdminService rbacConfigAdminService(RbacRoleRepository roleRepository,
                                                         RbacApiRepository apiRepository,
                                                         RbacRoleApiRepository roleApiRepository) {
        return new RbacConfigAdminService(roleRepository, apiRepository, roleApiRepository);
    }

    /** 无 Redis 时使用：角色校验直接查库 */
    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RbacRoleCache.class)
    public RbacRoleCache noOpRbacRoleCache() {
        return new NoOpRbacRoleCache();
    }

    /** 有 Redis 时使用：角色校验走缓存，TTL 与失效策略见 rbac.check.redis / rbac.cache */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnBean(org.springframework.data.redis.core.StringRedisTemplate.class)
    public RbacRoleCache redisRbacRoleCache(ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplate,
                                            RbacProperties properties) {
        org.springframework.data.redis.core.StringRedisTemplate template = redisTemplate.getIfAvailable();
        if (template == null) return new NoOpRbacRoleCache();
        return new com.sun.easyrbac.cache.RedisRbacRoleCache(template, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RbacUserRoleService rbacUserRoleService(RbacRoleRepository roleRepository,
                                                   RbacUserRoleRepository userRoleRepository,
                                                   RbacRoleApiRepository roleApiRepository,
                                                   RbacApiRepository apiRepository,
                                                   ObjectProvider<RbacRoleCache> roleCache) {
        return new RbacUserRoleService(roleRepository, userRoleRepository, roleApiRepository, apiRepository, roleCache.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac.check", name = "enabled", havingValue = "true")
    @ConditionalOnProperty(prefix = "rbac.check", name = "type", havingValue = "internal")
    public RbacTokenValidator internalRbacTokenValidator(RbacProperties properties) {
        return new InternalTokenValidator(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac.check", name = "enabled", havingValue = "true")
    @ConditionalOnProperty(prefix = "rbac.check", name = "type", havingValue = "jwt")
    public RbacTokenValidator jwtRbacTokenValidator(RbacProperties properties) {
        return new JwtTokenValidator(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac.check", name = "enabled", havingValue = "true")
    @ConditionalOnBean(RbacTokenValidator.class)
    public FilterRegistrationBean<RbacCheckFilter> rbacCheckFilterRegistration(RbacProperties properties,
                                                                                RbacTokenValidator tokenValidator,
                                                                                RbacRoleRepository roleRepository,
                                                                                RbacUserRoleRepository userRoleRepository,
                                                                                RbacRoleApiRepository roleApiRepository,
                                                                                RbacApiRepository apiRepository,
                                                                                List<RbacExcludePathCustomizer> excludePathCustomizers) {
        FilterRegistrationBean<RbacCheckFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RbacCheckFilter(properties, tokenValidator, roleRepository, userRoleRepository, roleApiRepository, apiRepository, excludePathCustomizers));
        reg.addUrlPatterns("/*");
        reg.setOrder(100);
        return reg;
    }

    @Bean("rbacExcludePathCustomizers")
    @ConditionalOnProperty(prefix = "rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
    public List<RbacExcludePathCustomizer> rbacExcludePathCustomizers(ApplicationContext applicationContext) {
        try {
            return new ArrayList<>(applicationContext.getBeansOfType(RbacExcludePathCustomizer.class).values());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "rbac.check", name = "enabled", havingValue = "true")
    @ConditionalOnBean(RbacTokenValidator.class)
    public WebMvcConfigurer rbacInterceptorConfigurer(RbacProperties properties, RbacUserRoleService userRoleService) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new RbacCheckInterceptor(properties, userRoleService)).addPathPatterns("/**");
            }
        };
    }
}
