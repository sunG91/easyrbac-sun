package com.sun.easyrbac.interceptor;

import com.sun.easyrbac.annotation.RbacController;
import com.sun.easyrbac.annotation.RbacMethod;
import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.service.RbacUserRoleService;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色校验拦截器：在 Filter 通过 Token 后，根据当前接口所需角色判断用户是否具备（满足其一即通过）。
 * 支持 intercept-mode: annotated（仅对带 @RbacController / @RbacMethod 的接口校验，未标注则放行）。
 *
 * @author SUNRUI
 */
public class RbacCheckInterceptor implements HandlerInterceptor {

    private final RbacProperties properties;
    private final RbacUserRoleService userRoleService;

    public RbacCheckInterceptor(RbacProperties properties, RbacUserRoleService userRoleService) {
        this.properties = properties;
        this.userRoleService = userRoleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled() || !properties.getCheck().isEnabled()) {
            return true;
        }
        Object userIdObj = request.getAttribute("rbac.userId");
        if (userIdObj == null) {
            return true;
        }
        String userId = userIdObj.toString();
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        List<String> requiredRoleCodes = resolveRequiredRoles(hm);
        if (requiredRoleCodes.isEmpty()) {
            return true;
        }
        for (String roleCode : requiredRoleCodes) {
            if (userRoleService.hasRole(userId, roleCode)) {
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + RbacConstants.ERR_ACCESS_DENIED + "\",\"message\":\"Access denied\"}");
        return false;
    }

    private List<String> resolveRequiredRoles(HandlerMethod hm) {
        RbacMethod methodRbac = AnnotationUtils.findAnnotation(hm.getMethod(), RbacMethod.class);
        String methodRole = resolveRoleFromRbacMethod(methodRbac);
        if (methodRole != null && !methodRole.isEmpty()) {
            return Collections.singletonList(methodRole);
        }
        RbacController classRbac = AnnotationUtils.findAnnotation(hm.getBeanType(), RbacController.class);
        String classRole = resolveRoleFromRbacController(classRbac);
        if (classRole != null && !classRole.isEmpty()) {
            return Collections.singletonList(classRole);
        }
        return Collections.emptyList();
    }

    private String resolveRoleFromRbacMethod(RbacMethod a) {
        if (a == null) return null;
        if (a.value() != null && !a.value().isEmpty()) return a.value();
        if (a.id() != null && !a.id().isEmpty()) return a.id();
        if (a.path() != null && !a.path().isEmpty()) return a.path();
        return null;
    }

    private String resolveRoleFromRbacController(RbacController a) {
        if (a == null) return null;
        if (a.value() != null && !a.value().isEmpty()) return a.value();
        if (a.id() != null && !a.id().isEmpty()) return a.id();
        if (a.path() != null && !a.path().isEmpty()) return a.path();
        return null;
    }
}
