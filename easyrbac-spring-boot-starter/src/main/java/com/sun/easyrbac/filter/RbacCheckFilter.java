package com.sun.easyrbac.filter;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import com.sun.easyrbac.core.repository.RbacUserRoleRepository;
import com.sun.easyrbac.core.token.RbacTokenValidator;
import com.sun.easyrbac.ext.RbacExcludePathCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RBAC 校验过滤器：从请求头取 Token，校验通过后检查当前请求路径所需角色是否包含用户角色。
 * 支持 exclude-paths、intercept-mode: annotated。
 *
 * @author SUNRUI
 */
public class RbacCheckFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RbacCheckFilter.class);

    private final RbacProperties properties;
    private final RbacTokenValidator tokenValidator;
    private final RbacRoleRepository roleRepository;
    private final RbacUserRoleRepository userRoleRepository;
    private final RbacRoleApiRepository roleApiRepository;
    private final RbacApiRepository apiRepository;
    private final List<RbacExcludePathCustomizer> excludePathCustomizers;
    private final UrlPathHelper pathHelper = new UrlPathHelper();
    private final StringRedisTemplate redisTemplate;

    public RbacCheckFilter(RbacProperties properties,
                           RbacTokenValidator tokenValidator,
                           RbacRoleRepository roleRepository,
                           RbacUserRoleRepository userRoleRepository,
                           RbacRoleApiRepository roleApiRepository,
                           RbacApiRepository apiRepository,
                           List<RbacExcludePathCustomizer> excludePathCustomizers,
                           StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.tokenValidator = tokenValidator;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleApiRepository = roleApiRepository;
        this.apiRepository = apiRepository;
        this.excludePathCustomizers = excludePathCustomizers != null ? excludePathCustomizers : new ArrayList<>();
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!properties.isEnabled() || !properties.getCheck().isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String requestPath = pathHelper.getLookupPathForRequest(request);
        if (isExcludePath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        String token = resolveToken(request);
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            String msg = "Missing token: set header " + (properties.getCheck().getType().equalsIgnoreCase("internal")
                    ? properties.getCheck().getInternal().getHeader()
                    : properties.getCheck().getJwt().getHeader())
                    + " with prefix \"" + (properties.getCheck().getType().equalsIgnoreCase("internal")
                    ? properties.getCheck().getInternal().getPrefix()
                    : properties.getCheck().getJwt().getPrefix()) + " <token>\"";
            response.getWriter().write("{\"code\":\"RBAC_3002\",\"message\":\"" + escapeJson(msg) + "\"}");
            return;
        }
        String userId = tokenValidator.validate(token);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"RBAC_3002\",\"message\":\"Token invalid or expired\"}");
            return;
        }
        if (!checkLoginStateInRedis(token, userId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"" + RbacConstants.ERR_TOKEN_INVALID + "\",\"message\":\"Login state not found in Redis\"}");
            return;
        }
        request.setAttribute("rbac.userId", userId);
        chain.doFilter(request, response);
    }

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private boolean isExcludePath(String path) {
        List<String> excludes = new ArrayList<>(properties.getCheck().getExcludePaths());
        for (RbacExcludePathCustomizer c : excludePathCustomizers) {
            if (c.getExcludePaths() != null) {
                excludes.addAll(c.getExcludePaths());
            }
        }
        for (String pattern : excludes) {
            if (pattern != null && !pattern.isEmpty() && PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLoginStateInRedis(String token, String userId) {
        if (redisTemplate == null) {
            // 未启用 Redis，则不做登录态强校验
            return true;
        }
        try {
            RbacProperties.Redis redis = properties.getCheck().getRedis();
            String prefix = redis.getKeyPrefix() != null ? redis.getKeyPrefix() : "rbac:login:";
            String keyBy = redis.getKeyBy() != null ? redis.getKeyBy() : "user_id";
            String key;
            if ("token".equalsIgnoreCase(keyBy)) {
                key = prefix + token;
            } else {
                key = prefix + userId;
            }
            String value = redisTemplate.opsForValue().get(key);
            return value != null && !value.isEmpty();
        } catch (Exception e) {
            log.warn("[EasyRBAC] Check login state in Redis error: {}", e.getMessage());
            return false;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveToken(HttpServletRequest request) {
        String headerName = "internal".equalsIgnoreCase(properties.getCheck().getType())
                ? properties.getCheck().getInternal().getHeader()
                : properties.getCheck().getJwt().getHeader();
        String prefix = "internal".equalsIgnoreCase(properties.getCheck().getType())
                ? properties.getCheck().getInternal().getPrefix()
                : properties.getCheck().getJwt().getPrefix();
        String raw = request.getHeader(headerName);
        if (raw != null && prefix != null && !prefix.isEmpty() && raw.startsWith(prefix)) {
            return raw.substring(prefix.length()).trim();
        }
        return raw;
    }
}
