package com.sun.easyrbac.interceptor;

import com.sun.easyrbac.annotation.RbacController;
import com.sun.easyrbac.annotation.RbacMethod;
import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.service.RbacUserRoleService;
import com.sun.easyrbac.util.RbacRoleEnumExtractor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

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
        List<String> methodRoles = resolveRolesFromRbacMethod(methodRbac);
        if (!methodRoles.isEmpty()) {
            return methodRoles;
        }
        RbacController classRbac = AnnotationUtils.findAnnotation(hm.getBeanType(), RbacController.class);
        List<String> classRoles = resolveRolesFromRbacController(classRbac);
        if (!classRoles.isEmpty()) {
            return classRoles;
        }
        return Collections.emptyList();
    }

    private List<String> resolveRolesFromRbacMethod(RbacMethod a) {
        if (a == null) return Collections.emptyList();
        List<String> base = mergeNonEmpty(a.value(), a.id(), a.path());
        List<String> enumCodes = RbacRoleEnumExtractor.extractCodes(a.roleEnums());
        if (enumCodes.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return enumCodes;
        }
        List<String> merged = new java.util.ArrayList<>(base.size() + enumCodes.size());
        merged.addAll(base);
        merged.addAll(enumCodes);
        return merged;
    }

    private List<String> resolveRolesFromRbacController(RbacController a) {
        if (a == null) return Collections.emptyList();
        List<String> base = mergeNonEmpty(a.value(), a.id(), a.path());
        List<String> enumCodes = RbacRoleEnumExtractor.extractCodes(a.roleEnums());
        if (enumCodes.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return enumCodes;
        }
        List<String> merged = new java.util.ArrayList<>(base.size() + enumCodes.size());
        merged.addAll(base);
        merged.addAll(enumCodes);
        return merged;
    }

    private List<String> mergeNonEmpty(String[]... sources) {
        List<String> result = new java.util.ArrayList<>();
        for (String[] arr : sources) {
            if (arr == null) continue;
            for (String v : arr) {
                if (v == null) continue;
                String trimmed = v.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }
}
