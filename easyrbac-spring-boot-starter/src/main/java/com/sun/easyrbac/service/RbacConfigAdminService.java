package com.sun.easyrbac.service;

import com.sun.easyrbac.core.domain.RbacApi;
import com.sun.easyrbac.core.domain.RbacRole;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * 运行时配置管理：角色/接口/角色-接口关系的增删与 YAML 导入导出。
 * 用于文档中提到的 addRoleDefinition / addApi / bindRoleApi / importFromYaml / exportToYaml 能力。
 *
 * YAML 结构示例：
 *
 * roles:
 *   - code: "10000"
 *     name: "超级管理员"
 * apis:
 *   - path: "/user/list"
 *     method: "GET"
 *     controller: "UserController"
 * role-apis:
 *   - role-code: "10000"
 *     api-path: "/user/list"
 *
 * @author SUNRUI
 */
public class RbacConfigAdminService {

    private final RbacRoleRepository roleRepository;
    private final RbacApiRepository apiRepository;
    private final RbacRoleApiRepository roleApiRepository;

    public RbacConfigAdminService(RbacRoleRepository roleRepository,
                                  RbacApiRepository apiRepository,
                                  RbacRoleApiRepository roleApiRepository) {
        this.roleRepository = roleRepository;
        this.apiRepository = apiRepository;
        this.roleApiRepository = roleApiRepository;
    }

    /** 在运行时新增或更新角色定义（code 已存在则更新名称） */
    public void addRoleDefinition(String roleCode, String roleName) {
        roleRepository.findByRoleCode(roleCode).ifPresentOrElse(
                role -> {
                    role.setRoleName(roleName);
                    roleRepository.save(role);
                },
                () -> {
                    RbacRole r = new RbacRole();
                    r.setRoleCode(roleCode);
                    r.setRoleName(roleName);
                    roleRepository.save(r);
                }
        );
    }

    /** 在运行时新增接口（path 唯一），存在则返回已有记录 */
    public RbacApi addApi(String path, String httpMethod, String controllerName) {
        return apiRepository.findByPath(path).orElseGet(() -> {
            RbacApi api = new RbacApi();
            api.setPath(path);
            api.setMethod(httpMethod != null ? httpMethod : "");
            api.setControllerName(controllerName != null ? controllerName : "");
            apiRepository.save(api);
            return api;
        });
    }

    /** 绑定角色与接口（按角色编码与接口 path） */
    public void bindRoleApi(String roleCode, String apiPath) {
        Optional<RbacRole> roleOpt = roleRepository.findByRoleCode(roleCode);
        Optional<RbacApi> apiOpt = apiRepository.findByPath(apiPath);
        if (roleOpt.isEmpty() || apiOpt.isEmpty()) {
            return;
        }
        roleApiRepository.bindRoleApi(roleOpt.get().getId(), apiOpt.get().getId());
    }

    /** 从 YAML 导入角色、接口与角色-接口关联（幂等增量：已有记录不删除） */
    @SuppressWarnings("unchecked")
    public void importFromYaml(InputStream input) throws IOException {
        if (input == null) {
            return;
        }
        Yaml yaml = new Yaml();
        Object root = yaml.load(input);
        if (!(root instanceof Map)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) root;

        Object rolesObj = map.getOrDefault("roles", Collections.emptyList());
        if (rolesObj instanceof Iterable) {
            for (Object r : (Iterable<?>) rolesObj) {
                if (r instanceof Map) {
                    Map<?, ?> rm = (Map<?, ?>) r;
                    String code = String.valueOf(rm.get("code"));
                    Object nameObj = rm.get("name");
                    String name = nameObj != null ? String.valueOf(nameObj) : "";
                    if (code != null && !code.isEmpty()) {
                        addRoleDefinition(code, name);
                    }
                }
            }
        }

        Map<String, RbacApi> pathToApi = new HashMap<>();
        for (RbacApi api : apiRepository.findAll()) {
            pathToApi.put(api.getPath(), api);
        }

        Object apisObj = map.getOrDefault("apis", Collections.emptyList());
        if (apisObj instanceof Iterable) {
            for (Object a : (Iterable<?>) apisObj) {
                if (a instanceof Map) {
                    Map<?, ?> am = (Map<?, ?>) a;
                    String path = String.valueOf(am.get("path"));
                    if (path == null || path.isEmpty()) {
                        continue;
                    }
                    String method = am.get("method") != null ? String.valueOf(am.get("method")) : "";
                    String controller = am.get("controller") != null ? String.valueOf(am.get("controller")) : "";
                    RbacApi api = addApi(path, method, controller);
                    pathToApi.put(path, api);
                }
            }
        }

        Object roleApisObj = map.getOrDefault("role-apis", Collections.emptyList());
        if (roleApisObj instanceof Iterable) {
            for (Object ra : (Iterable<?>) roleApisObj) {
                if (ra instanceof Map) {
                    Map<?, ?> ram = (Map<?, ?>) ra;
                    String roleCode = String.valueOf(ram.get("role-code"));
                    String apiPath = String.valueOf(ram.get("api-path"));
                    if (roleCode == null || roleCode.isEmpty() || apiPath == null || apiPath.isEmpty()) {
                        continue;
                    }
                    bindRoleApi(roleCode, apiPath);
                }
            }
        }
    }

    /** 将当前库中的角色、接口与角色-接口关联导出为 YAML（便于备份/迁移） */
    public void exportToYaml(OutputStream out) throws IOException {
        List<RbacRole> roles = roleRepository.findAll();
        List<RbacApi> apis = apiRepository.findAll();

        List<Map<String, Object>> roleList = new ArrayList<>();
        for (RbacRole r : roles) {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("code", r.getRoleCode());
            rm.put("name", r.getRoleName());
            roleList.add(rm);
        }

        List<Map<String, Object>> apiList = new ArrayList<>();
        for (RbacApi a : apis) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("path", a.getPath());
            am.put("method", a.getMethod());
            am.put("controller", a.getControllerName());
            apiList.add(am);
        }

        List<Map<String, Object>> roleApiList = new ArrayList<>();
        for (RbacRole r : roles) {
            List<Long> apiIds = roleApiRepository.findApiIdsByRoleId(r.getId());
            for (Long apiId : apiIds) {
                for (RbacApi a : apis) {
                    if (a.getId() != null && a.getId().equals(apiId)) {
                        Map<String, Object> ram = new LinkedHashMap<>();
                        ram.put("role-code", r.getRoleCode());
                        ram.put("api-path", a.getPath());
                        roleApiList.add(ram);
                    }
                }
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("roles", roleList);
        root.put("apis", apiList);
        root.put("role-apis", roleApiList);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        yaml.dump(root, new java.io.OutputStreamWriter(out));
    }
}

