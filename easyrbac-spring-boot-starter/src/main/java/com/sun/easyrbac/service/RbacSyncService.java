package com.sun.easyrbac.service;

import com.sun.easyrbac.annotation.RbacController;
import com.sun.easyrbac.annotation.RbacMethod;
import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.core.domain.RbacApi;
import com.sun.easyrbac.core.domain.RbacRole;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 启动时同步：从 role-mapping 同步角色表，从带 RBAC 注解的 Controller 扫描接口并同步接口表、角色-接口关联表。
 * 有变更才更新，不删用户已有数据。
 *
 * @author SUNRUI
 */
public class RbacSyncService {

    private static final Logger log = LoggerFactory.getLogger(RbacSyncService.class);

    private final RbacProperties properties;
    private final RbacRoleRepository roleRepository;
    private final RbacApiRepository apiRepository;
    private final RbacRoleApiRepository roleApiRepository;
    private final ApplicationContext applicationContext;

    public RbacSyncService(RbacProperties properties,
                           RbacRoleRepository roleRepository,
                           RbacApiRepository apiRepository,
                           RbacRoleApiRepository roleApiRepository,
                           ApplicationContext applicationContext) {
        this.properties = properties;
        this.roleRepository = roleRepository;
        this.apiRepository = apiRepository;
        this.roleApiRepository = roleApiRepository;
        this.applicationContext = applicationContext;
    }

    /**
     * 执行同步：角色表、接口表、角色-接口表（可选异步由调用方控制）。
     * 角色来源：优先 YAML role-mapping；若为空则从 rbac.auto.role-enum-class 指定枚举解析。
     */
    public void sync() {
        if (!properties.isEnabled() || !properties.getAuto().isAutoSyncData()) {
            return;
        }
        Map<String, String> roleMapping = properties.getAuto().getRoleMapping();
        if (roleMapping == null || roleMapping.isEmpty()) {
            roleMapping = resolveRoleMappingFromEnum();
        }
        if (roleMapping == null || roleMapping.isEmpty()) {
            log.warn("[EasyRBAC] role-mapping 为空且未配置有效 role-enum-class，跳过同步");
            return;
        }

        if (properties.getAuto().isSyncRoleTable()) {
            syncRoles(roleMapping);
        }
        Map<String, Set<String>> apiToRoleCodes = collectApiToRoleCodesFromControllers();
        if (properties.getAuto().isSyncApiTable()) {
            syncApis(apiToRoleCodes);
        }
        if (properties.getAuto().isSyncRoleApiTable()) {
            syncRoleApi(apiToRoleCodes);
        }
        log.info("[EasyRBAC] 启动同步完成");
    }

    private void syncRoles(Map<String, String> roleMapping) {
        for (Map.Entry<String, String> e : roleMapping.entrySet()) {
            String code = e.getKey();
            String name = e.getValue();
            roleRepository.findByRoleCode(code).ifPresentOrElse(
                    role -> {
                        if (!name.equals(role.getRoleName())) {
                            role.setRoleName(name);
                            roleRepository.save(role);
                        }
                    },
                    () -> {
                        RbacRole r = new RbacRole();
                        r.setRoleCode(code);
                        r.setRoleName(name);
                        roleRepository.save(r);
                    }
            );
        }
    }

    /**
     * 从 rbac.auto.role-enum-class 指定的枚举类解析角色映射（编码 -> 名称）。
     * 枚举常量需标注 @RbacRole(value="编码", name="名称")，name 为空时使用枚举常量名。
     */
    private Map<String, String> resolveRoleMappingFromEnum() {
        String enumClass = properties.getAuto().getRoleEnumClass();
        if (enumClass == null || enumClass.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            ClassLoader loader = applicationContext.getClassLoader();
            Class<?> clazz = Class.forName(enumClass.trim(), false, loader != null ? loader : Thread.currentThread().getContextClassLoader());
            if (!clazz.isEnum()) {
                log.warn("[EasyRBAC] role-enum-class 不是枚举类: {}", enumClass);
                return new LinkedHashMap<>();
            }
            Object[] constants = clazz.getEnumConstants();
            if (constants == null) {
                return new LinkedHashMap<>();
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (Object c : constants) {
                com.sun.easyrbac.annotation.RbacRole ann = c.getClass().getField(((Enum<?>) c).name()).getAnnotation(com.sun.easyrbac.annotation.RbacRole.class);
                if (ann == null) continue;
                String code = ann.value();
                if (code == null || code.isEmpty()) continue;
                String name = (ann.name() != null && !ann.name().isEmpty()) ? ann.name() : ((Enum<?>) c).name();
                map.put(code, name);
            }
            if (!map.isEmpty()) {
                log.info("[EasyRBAC] 从枚举解析角色数: {} (class={})", map.size(), enumClass);
            }
            return map;
        } catch (ClassNotFoundException e) {
            log.warn("[EasyRBAC] 未找到 role-enum-class: {}", enumClass, e);
            return new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("[EasyRBAC] 解析 role-enum-class 失败: {}", enumClass, e);
            return new LinkedHashMap<>();
        }
    }

    /** 扫描所有 Controller，收集 (apiPath -> 需要的角色编码集合) */
    private Map<String, Set<String>> collectApiToRoleCodesFromControllers() {
        Map<String, Set<String>> apiPathToRoles = new LinkedHashMap<>();
        String scanPackages = properties.getAuto().getScanPackages();
        String pathFormat = properties.getAuto().getApiPathFormat() != null ? properties.getAuto().getApiPathFormat() : RbacConstants.API_PATH_FORMAT_LOWERCASE_UNDERSCORE;

        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Controller.class);
        controllers.putAll(applicationContext.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController.class));

        String[] packages = (scanPackages != null && !scanPackages.isEmpty())
                ? scanPackages.trim().split("\\s*,\\s*") : null;

        for (Object bean : controllers.values()) {
            Class<?> clazz = bean.getClass();
            if (packages != null && packages.length > 0) {
                String pkg = clazz.getPackageName();
                boolean inScope = false;
                for (String p : packages) {
                    if (pkg.startsWith(p.trim())) { inScope = true; break; }
                }
                if (!inScope) continue;
            }
            RbacController classRbac = AnnotationUtils.findAnnotation(clazz, RbacController.class);
            String classRoleId = resolveRoleFromAnnotation(classRbac);

            RequestMapping classReq = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
            String classPathPrefix = (classReq != null && classReq.value().length > 0) ? classReq.value()[0] : "";

            for (Method method : clazz.getMethods()) {
                RbacMethod methodRbac = AnnotationUtils.findAnnotation(method, RbacMethod.class);
                String methodRoleId = resolveRoleFromAnnotation(methodRbac);
                String roleCode = (methodRoleId != null && !methodRoleId.isEmpty()) ? methodRoleId : classRoleId;
                if (roleCode == null || roleCode.isEmpty()) {
                    continue;
                }

                String httpMethod = resolveHttpMethod(method);
                String path = buildApiPath(clazz, method, classPathPrefix, pathFormat);
                if (path == null) {
                    continue;
                }
                apiPathToRoles.computeIfAbsent(path, k -> new HashSet<>()).add(roleCode);
            }
        }
        return apiPathToRoles;
    }

    private String resolveRoleFromAnnotation(RbacController a) {
        if (a == null) return null;
        if (a.value() != null && !a.value().isEmpty()) return a.value();
        if (a.id() != null && !a.id().isEmpty()) return a.id();
        if (a.path() != null && !a.path().isEmpty()) return a.path();
        return null;
    }

    private String resolveRoleFromAnnotation(RbacMethod a) {
        if (a == null) return null;
        if (a.value() != null && !a.value().isEmpty()) return a.value();
        if (a.id() != null && !a.id().isEmpty()) return a.id();
        if (a.path() != null && !a.path().isEmpty()) return a.path();
        return null;
    }

    private String resolveHttpMethod(Method method) {
        if (AnnotationUtils.findAnnotation(method, GetMapping.class) != null) return "GET";
        if (AnnotationUtils.findAnnotation(method, PostMapping.class) != null) return "POST";
        if (AnnotationUtils.findAnnotation(method, PutMapping.class) != null) return "PUT";
        if (AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null) return "DELETE";
        if (AnnotationUtils.findAnnotation(method, PatchMapping.class) != null) return "PATCH";
        return "GET";
    }

    private String buildApiPath(Class<?> controllerClass, Method method, String classPathPrefix, String pathFormat) {
        String controllerName = controllerClass.getSimpleName();
        String methodName = method.getName();
        String httpMethod = resolveHttpMethod(method);
        String methodPathSegment = getMethodPathSegment(method);
        if (RbacConstants.API_PATH_FORMAT_PATH.equals(pathFormat)) {
            String full = (classPathPrefix + "/" + methodPathSegment).replace("//", "/");
            return full.isEmpty() ? "/" : full;
        }
        if (RbacConstants.API_PATH_FORMAT_CAMEL_CASE.equals(pathFormat)) {
            String c = controllerName.length() > 0 ? controllerName.substring(0, 1).toLowerCase() + controllerName.substring(1) : "";
            String m = methodName.length() > 0 ? methodName.substring(0, 1).toUpperCase() + methodName.substring(1) : "";
            return c + m + httpMethod;
        }
        return (controllerName + "_" + methodName + "_" + httpMethod).toLowerCase().replace("-", "_");
    }

    private String getMethodPathSegment(Method method) {
        GetMapping g = AnnotationUtils.findAnnotation(method, GetMapping.class);
        if (g != null && g.value().length > 0) return g.value()[0];
        PostMapping p = AnnotationUtils.findAnnotation(method, PostMapping.class);
        if (p != null && p.value().length > 0) return p.value()[0];
        PutMapping u = AnnotationUtils.findAnnotation(method, PutMapping.class);
        if (u != null && u.value().length > 0) return u.value()[0];
        DeleteMapping d = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
        if (d != null && d.value().length > 0) return d.value()[0];
        PatchMapping h = AnnotationUtils.findAnnotation(method, PatchMapping.class);
        if (h != null && h.value().length > 0) return h.value()[0];
        RequestMapping r = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (r != null && r.value().length > 0) return r.value()[0];
        return method.getName();
    }

    private void syncApis(Map<String, Set<String>> apiPathToRoles) {
        for (String path : apiPathToRoles.keySet()) {
            apiRepository.findByPath(path).ifPresentOrElse(
                    api -> { /* 已存在，仅关联在 syncRoleApi 中处理 */ },
                    () -> {
                        RbacApi api = new RbacApi();
                        api.setPath(path);
                        api.setMethod("");
                        api.setControllerName("");
                        apiRepository.save(api);
                    }
            );
        }
    }

    private void syncRoleApi(Map<String, Set<String>> apiPathToRoles) {
        for (Map.Entry<String, Set<String>> e : apiPathToRoles.entrySet()) {
            String path = e.getKey();
            Set<String> roleCodes = e.getValue();
            apiRepository.findByPath(path).ifPresent(api -> {
                for (String roleCode : roleCodes) {
                    roleRepository.findByRoleCode(roleCode).ifPresent(role ->
                            roleApiRepository.bindRoleApi(role.getId(), api.getId())
                    );
                }
            });
        }
    }
}
