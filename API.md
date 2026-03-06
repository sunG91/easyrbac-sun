# EasyRBAC API 文档

框架对外可用的服务、扩展点、注解和常量，按使用频率整理。需要查方法签名或参数时直接翻这里。

---

## 业务服务（直接注入用）

框架自动注册，`@Autowired` 注入即可。

---

### 1. RbacTokenIssuerService

**包名**：`com.sun.easyrbac.service.RbacTokenIssuerService`

**作用**：登录后签发 Token。业务在「登录校验通过」后只需传入用户标识（String/Long/Integer 等），即可拿到与当前 `rbac.check` 配置一致的 Token，无需自实现 HMAC/JWT。用户标识类型可在 yaml 中配置 `rbac.check.internal.user-identifier-type`（string/long/integer），仅作约定说明，实际传入任意类型均可。

**注入方式**：
```java
@Autowired
private RbacTokenIssuerService rbacTokenIssuerService;
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `issueToken(Object userId)` | `RbacTokenResult` 或 `null` | `userId`：用户唯一标识，可为 String、Long、Integer 等 | 为指定用户签发 Token。`type=internal` 时返回完整结果（内部统一转为字符串存储）；`type=jwt` 时返回 `null`，需业务用自有 JWT 库签发。 |
| `issueTokenAndCache(Object userId)` | `RbacTokenResult` 或 `null` | `userId`：用户唯一标识 | 为指定用户签发 Token，并在启用 Redis 时将登录态与当前角色列表写入 Redis。登录态 key 由 `rbac.check.redis.key-prefix` 与 `key-by` 决定（默认 `rbac:login:userId`），value 为包含 `userId` 与 `roles` 的 JSON。 |
| `issueTokenAndCache(Object userId, Object extra)` | `RbacTokenResult` 或 `null` | `userId`：用户唯一标识；`extra`：扩展对象 | 同上，同时将 `extra.toString()` 写入 JSON 的 `extra` 字段，便于记录设备、IP 等额外信息。 |

---

### 2. RbacUserRoleService

**包名**：`com.sun.easyrbac.service.RbacUserRoleService`

**作用**：操作用户与角色：添加/移除角色、查询用户角色、判断是否拥有某角色或某权限。无 Redis 时直接查库；有 Redis 时走角色缓存（getRoles/hasRole 先读缓存，addRole/removeRole 后失效对应用户缓存）。

**注入方式**：
```java
@Autowired
private RbacUserRoleService rbacUserRoleService;
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `addRole(String userId, String roleCode)` | `void` | `userId`：用户 ID；`roleCode`：角色编码 | 为指定用户绑定单个角色。若启用 Redis 则会失效该用户角色缓存。 |
| `addRoles(String userId, List<String> roleCodes)` | `void` | `userId`：用户 ID；`roleCodes`：角色编码列表 | 批量绑定角色；若启用缓存则失效该用户缓存。`roleCodes` 为 null 时直接返回。 |
| `removeRole(String userId, String roleCode)` | `void` | `userId`：用户 ID；`roleCode`：角色编码 | 解除用户与某角色的绑定；若启用缓存则失效该用户缓存。 |
| `removeRoles(String userId, List<String> roleCodes)` | `void` | `userId`：用户 ID；`roleCodes`：角色编码列表 | 批量解除角色；若启用缓存则逐角色解除并失效该用户缓存。 |
| `getRoles(String userId)` | `List<String>` | `userId`：用户 ID | 返回该用户当前绑定的角色编码列表。有 Redis 时先读缓存，未命中再查库并回填缓存。 |
| `hasRole(String userId, String roleCode)` | `boolean` | `userId`：用户 ID；`roleCode`：角色编码 | 判断用户是否拥有某角色；有 Redis 时通过 getRoles（走缓存）判断。 |
| `hasPermission(String userId, String permissionIdOrPath)` | `boolean` | `userId`：用户 ID；`permissionIdOrPath`：接口 path 或权限 ID | 判断用户是否拥有某权限（按权限 path 或权限 ID 对应的接口）。内部查用户角色 → 角色关联的接口 → 是否包含该 path。 |

---

### 3. RbacConfigAdminService

**包名**：`com.sun.easyrbac.service.RbacConfigAdminService`

**作用**：运行时管理角色、接口及其绑定关系，支持 YAML 导入导出。适合做管理后台、配置备份。

**注入方式**：
```java
@Autowired
private RbacConfigAdminService rbacConfigAdminService;
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `addRoleDefinition(String roleCode, String roleName)` | `void` | `roleCode`：角色编码；`roleName`：角色名称 | 在运行时新增或更新角色定义。code 已存在则更新名称，否则新增。 |
| `addApi(String path, String httpMethod, String controllerName)` | `RbacApi` | `path`：接口 path；`httpMethod`：如 GET/POST；`controllerName`：控制器名 | 在运行时新增接口（path 唯一）。若已存在则返回已有记录，否则创建并保存。 |
| `bindRoleApi(String roleCode, String apiPath)` | `void` | `roleCode`：角色编码；`apiPath`：接口 path | 绑定角色与接口（按角色编码与接口 path）。若角色或接口不存在则直接返回。 |
| `importFromYaml(InputStream input)` | `void` | `input`：YAML 输入流 | 从 YAML 导入角色、接口与角色-接口关联。幂等增量：已有记录不删除，只增补。YAML 结构见下方。 |
| `exportToYaml(OutputStream out)` | `void` | `out`：输出流 | 将当前库中的角色、接口与角色-接口关联导出为 YAML，便于备份/迁移。 |

**YAML 导入结构示例**：
```yaml
roles:
  - code: "10000"
    name: "超级管理员"
apis:
  - path: "/user/list"
    method: "GET"
    controller: "UserController"
role-apis:
  - role-code: "10000"
    api-path: "/user/list"
```

---

## 二、可选注入（高级用法）

---

### 4. RbacTokenValidator

**包名**：`com.sun.easyrbac.core.token.RbacTokenValidator`

**作用**：Token 校验与签发。由框架根据 `rbac.check.type` 注入 internal 或 jwt 实现；业务也可自定义实现并注册为 Bean 替代默认。

**注入方式**（使用框架默认实现时）：
```java
@Autowired
private RbacTokenValidator rbacTokenValidator;
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `validate(String token)` | `String` 或 `null` | `token`：请求中解析出的 token 字符串 | 校验 token 是否有效（签名、未过期等）。有效则返回解析出的用户标识（如 userId），否则返回 null。 |
| `generate(Object userId)` | `String` 或 `null` | `userId`：用户唯一标识，可为 String、Long、Integer 等，内部会转为字符串存储 | 签发 token，将用户标识写入，返回 token 字符串。Internal 实现会生成；JWT 实现当前返回 null，需业务用 JWT 库签发。 |

日常用 `RbacTokenIssuerService.issueToken(userId)` 即可，一般不用直接调 `generate`。

---

### 5. RbacSyncService

**包名**：`com.sun.easyrbac.service.RbacSyncService`

**作用**：启动时同步：从 role-mapping 或 role-enum-class 同步角色表，从带 RBAC 注解的 Controller 扫描并同步接口表、角色-接口表。一般由框架在启动时自动执行，业务通常无需直接调用。

**注入方式**（如需手动触发同步时）：
```java
@Autowired
private RbacSyncService rbacSyncService;
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `sync()` | `void` | 无 | 执行同步：角色表、接口表、角色-接口表。角色来源优先 YAML role-mapping；若为空则从 rbac.auto.role-enum-class 指定枚举解析。有变更才更新，不删用户已有数据。 |

---

## 三、扩展点（实现接口并注册为 Spring Bean）

实现下列接口并注册为 Bean 后，框架会自动发现并应用。

---

### 6. RbacExcludePathCustomizer

**包名**：`com.sun.easyrbac.ext.RbacExcludePathCustomizer`

**作用**：动态返回不参与权限校验的路径列表，与 YAML `rbac.check.exclude-paths` 取并集。

**实现示例**：
```java
@Component
public class MyExcludePathCustomizer implements RbacExcludePathCustomizer {
    @Override
    public List<String> getExcludePaths() {
        return Arrays.asList("/login", "/public/**", "/actuator/**");
    }
}
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `getExcludePaths()` | `List<String>` | 无 | 返回放行的路径，支持 Ant 风格（如 `/public/**`）。实现内尽量返回内存或缓存结果，避免每次请求执行重量级操作。 |

---

### 7. RbacFieldMappingCustomizer

**包名**：`com.sun.easyrbac.ext.RbacFieldMappingCustomizer`

**作用**：在代码中自定义表字段映射（与 YAML field-mapping 可叠加）。实现此接口并注册为 Spring Bean，框架在解析列名时会调用。

**实现示例**：
```java
@Component
public class MyFieldMappingCustomizer implements RbacFieldMappingCustomizer {
    @Override
    public void customize(FieldMappingBuilder builder) {
        builder.mapIdTo("id").mapPathTo("api_path").addMapping("role_code", "role_code");
    }
}
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `customize(FieldMappingBuilder builder)` | `void` | `builder`：字段映射构建器 | 在 builder 上配置逻辑名到数据库列名的映射。必配：mapIdTo、mapPathTo 至少各配置一次。 |

---

### 8. FieldMappingBuilder

**包名**：`com.sun.easyrbac.ext.FieldMappingBuilder`

**作用**：供 `RbacFieldMappingCustomizer` 使用，链式配置逻辑名 → 列名。

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `mapIdTo(String columnName)` | `FieldMappingBuilder` | `columnName`：主键列名 | 逻辑名 id → 数据库列名。 |
| `mapPathTo(String columnName)` | `FieldMappingBuilder` | `columnName`：path 列名 | 逻辑名 path → 数据库列名。 |
| `addMapping(String logicalName, String columnName)` | `FieldMappingBuilder` | `logicalName`：逻辑名；`columnName`：列名 | 添加任意逻辑名 → 列名。 |
| `removeMapping(String logicalName)` | `FieldMappingBuilder` | `logicalName`：逻辑名 | 移除某逻辑名映射。 |
| `build()` | `Map<String, String>` | 无 | 返回当前映射的副本。 |

---

### 9. RbacStateTransitionPolicy

**包名**：`com.sun.easyrbac.core.ext.RbacStateTransitionPolicy`

**作用**：状态流转权限扩展点：判断从 fromState 到 toState 是否允许（如工单「待审核」→「已通过」仅审核员可操作）。框架不提供默认实现，用户实现后由**业务代码**在状态变更前调用。

**实现示例**：
```java
@Component
public class OrderStatePolicy implements RbacStateTransitionPolicy {
    @Override
    public boolean allowedTransition(String fromState, String toState, String roleCode) {
        if ("PENDING".equals(fromState) && "APPROVED".equals(toState)) {
            return "OPS".equals(roleCode) || "ADMIN".equals(roleCode);
        }
        return true;
    }
}
```

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `allowedTransition(String fromState, String toState, String roleCode)` | `boolean` | `fromState`：原状态；`toState`：目标状态；`roleCode`：当前角色编码 | 是否允许从 fromState 变更为 toState。true 允许，false 拒绝。业务在状态变更前调用，可传入 userId 再查角色。 |

---

## 四、注解

---

### 10. @RbacController

**包名**：`com.sun.easyrbac.annotation.RbacController`

**作用**：标注在 Controller 类上，表示该类下接口需要的默认角色/权限。可与 `@RbacMethod` 配合，方法级优先于类级。

**使用示例**：
```java
@RestController
@RequestMapping("/user")
@RbacController("10001")                 // 单个角色
public class UserController { ... }
```

支持多个角色/权限 ID，语义为「拥有任一即可访问」：

```java
@RestController
@RequestMapping("/user")
@RbacController({"10001","10002"})      // 拥有 10001 或 10002 之一即可访问
public class UserController { ... }
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | String 或 String[] | 默认需要的角色编码/权限 ID（与 id 等价），可为单个或多个。 |
| `id` | String 或 String[] | 显式指定权限 ID，可为单个或多个。 |
| `path` | String 或 String[] | 权限地址，仅 annotation-mode=path 时生效，可为单个或多个，须显式指定。 |

---

### 11. @RbacMethod

**包名**：`com.sun.easyrbac.annotation.RbacMethod`

**作用**：标注在 Controller 方法上，表示该接口需要的角色/权限。优先于类上的 `@RbacController`。

**使用示例**：
```java
@GetMapping("/list")
@RbacMethod("10000")  // 该接口需要角色 10000
public Result list() { ... }
```

也支持多个角色/权限 ID，语义同样是「拥有任一即可访问」：

```java
@GetMapping("/list")
@RbacMethod({"10000","10001"})          // 拥有 10000 或 10001 之一即可访问
public Result list() { ... }
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | String 或 String[] | 需要的角色编码/权限 ID（与 id 等价），可为单个或多个。 |
| `id` | String 或 String[] | 显式指定权限 ID，可为单个或多个。 |
| `path` | String 或 String[] | 权限地址，仅 annotation-mode=path 时生效，可为单个或多个。 |

---

### 12. @RbacRole

**包名**：`com.sun.easyrbac.annotation.RbacRole`

**作用**：标注在枚举常量上，表示该枚举对应的角色编码与角色名。配置 `rbac.auto.role-enum-class` 后，可从枚举同步角色表，此时可不配置 YAML 的 role-mapping。

**使用示例**：
```java
public enum AppRole {
    @RbacRole(value = "10000", name = "超级管理员")
    SUPER_ADMIN,
    @RbacRole(value = "10001", name = "普通用户")
    NORMAL_USER
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | String | 角色编码，如 "10000"。 |
| `name` | String | 角色名称（展示用）。为空时使用枚举常量名。 |

---

## 五、领域模型与 DTO

---

### 13. RbacRole

**包名**：`com.sun.easyrbac.core.domain.RbacRole`

**作用**：角色领域模型，对应角色表一行。

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `id` | Long | 主键。 |
| `roleCode` | String | 角色编码。 |
| `roleName` | String | 角色名称。 |
| `getId()` / `setId(Long)` | - | 主键 getter/setter。 |
| `getRoleCode()` / `setRoleCode(String)` | - | 角色编码。 |
| `getRoleName()` / `setRoleName(String)` | - | 角色名称。 |

---

### 14. RbacApi

**包名**：`com.sun.easyrbac.core.domain.RbacApi`

**作用**：接口/权限领域模型，对应接口表一行。path 为必配字段，用于权限校验与角色-接口关联。

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `id` | Long | 主键。 |
| `path` | String | 接口路径标识，必配。 |
| `method` | String | HTTP 方法。 |
| `controllerName` | String | 控制器名。 |
| `getId()` / `setId(Long)` | - | 主键。 |
| `getPath()` / `setPath(String)` | - | 接口 path。 |
| `getMethod()` / `setMethod(String)` | - | 方法。 |
| `getControllerName()` / `setControllerName(String)` | - | 控制器名。 |

---

### 15. RbacTokenResult

**包名**：`com.sun.easyrbac.core.model.RbacTokenResult`

**作用**：登录后签发 Token 的返回结果，可直接放入登录响应返回前端。

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `token` | String | Token 字符串。 |
| `type` | String | 类型，如 "Bearer"。 |
| `expireSeconds` | long | 过期秒数。 |
| `userId` | String | 用户 ID。 |
| `getToken()` / `setToken(String)` | - | Token。 |
| `getType()` / `setType(String)` | - | 类型。 |
| `getExpireSeconds()` / `setExpireSeconds(long)` | - | 过期秒数。 |
| `getUserId()` / `setUserId(String)` | - | 用户 ID。 |

---

## 六、仓储接口（内部用，可选）

core 层定义，starter 提供 JDBC 实现。一般通过 `RbacUserRoleService`、`RbacConfigAdminService` 间接用；要直接操作表可注入。

---

### 16. RbacRoleRepository

**包名**：`com.sun.easyrbac.core.repository.RbacRoleRepository`

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `findByRoleCode(String roleCode)` | `Optional<RbacRole>` | 角色编码 | 按角色编码查询。 |
| `findAll()` | `List<RbacRole>` | 无 | 查询所有角色。 |
| `save(RbacRole role)` | `void` | 角色实体 | 保存或更新角色。 |
| `saveAll(List<RbacRole> roles)` | `void` | 角色列表 | 批量保存或更新。 |

---

### 17. RbacApiRepository

**包名**：`com.sun.easyrbac.core.repository.RbacApiRepository`

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `findByPath(String path)` | `Optional<RbacApi>` | 接口 path | 按 path 查询。 |
| `findAll()` | `List<RbacApi>` | 无 | 查询所有接口。 |
| `save(RbacApi api)` | `void` | 接口实体 | 保存或更新。 |
| `saveAll(List<RbacApi> apis)` | `void` | 接口列表 | 批量保存。 |

---

### 18. RbacRoleApiRepository

**包名**：`com.sun.easyrbac.core.repository.RbacRoleApiRepository`

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `bindRoleApi(Long roleId, Long apiId)` | `void` | 角色 ID、接口 ID | 绑定角色与接口。 |
| `bindRoleApis(Long roleId, List<Long> apiIds)` | `void` | 角色 ID、接口 ID 列表 | 批量绑定。 |
| `findApiIdsByRoleId(Long roleId)` | `List<Long>` | 角色 ID | 查询某角色关联的接口 ID 列表。 |
| `findRoleIdsByApiId(Long apiId)` | `List<Long>` | 接口 ID | 查询某接口关联的角色 ID 列表。 |

---

### 19. RbacUserRoleRepository

**包名**：`com.sun.easyrbac.core.repository.RbacUserRoleRepository`

| 方法 | 返回值 | 参数 | 说明 |
|------|--------|------|------|
| `addRole(String userId, Long roleId)` | `void` | 用户 ID、角色 ID | 为用户绑定角色。 |
| `removeRole(String userId, Long roleId)` | `void` | 用户 ID、角色 ID | 解除用户与角色的绑定。 |
| `findRoleIdsByUserId(String userId)` | `List<Long>` | 用户 ID | 查询用户拥有的角色 ID 列表。 |
| `hasRole(String userId, Long roleId)` | `boolean` | 用户 ID、角色 ID | 判断用户是否拥有某角色。 |

---

## 七、常量与异常

---

### 20. RbacConstants

**包名**：`com.sun.easyrbac.constant.RbacConstants`

**作用**：框架全局常量，错误码、默认表名/列名、path 格式等。

| 常量名 | 值 | 说明 |
|--------|-----|------|
| `VERSION` | "1.0.0" | 当前框架版本。 |
| `ERR_CONFIG_REQUIRED` | "RBAC_1001" | 配置错误：缺少必配项。 |
| `ERR_FIELD_MAPPING` | "RBAC_1002" | 配置错误：表/字段映射缺少 id 或 path。 |
| `ERR_DB_OPERATION` | "RBAC_2001" | 数据库错误：建表或同步失败。 |
| `ERR_ACCESS_DENIED` | "RBAC_3001" | 校验失败：无权限。 |
| `ERR_TOKEN_INVALID` | "RBAC_3002" | 校验失败：Token 无效或过期。 |
| `DEFAULT_TABLE_ROLE` | "rbac_role" | 默认角色表名。 |
| `DEFAULT_TABLE_API` | "rbac_api" | 默认接口表名。 |
| `DEFAULT_TABLE_ROLE_API` | "rbac_role_api" | 默认角色-接口表名。 |
| `DEFAULT_TABLE_USER_ROLE` | "rbac_user_role" | 默认用户-角色表名。 |
| `API_PATH_FORMAT_LOWERCASE_UNDERSCORE` | "lowercase_underscore" | path 生成格式：小写+下划线。 |
| `API_PATH_FORMAT_CAMEL_CASE` | "camelCase" | path 生成格式：驼峰。 |
| `API_PATH_FORMAT_PATH` | "path" | path 生成格式：请求路径。 |

---

### 21. RbacException

**包名**：`com.sun.easyrbac.exception.RbacException`

**作用**：框架统一异常，携带错误码便于文档与排查。

| 构造/方法 | 说明 |
|-----------|------|
| `RbacException(String errorCode, String message)` | 使用错误码与消息构造。 |
| `RbacException(String errorCode, String message, Throwable cause)` | 带原因。 |
| `getErrorCode()` | 返回错误码，如 RBAC_3001。 |
| `getMessage()` | 返回消息。 |

---

## 八、快速索引（按使用场景）

| 场景 | 使用的 API |
|------|------------|
| 登录后签发 Token | `RbacTokenIssuerService.issueToken(userId)`，返回 `RbacTokenResult` |
| 为用户绑定/解除角色 | `RbacUserRoleService.addRole` / `addRoles` / `removeRole` / `removeRoles` |
| 查询用户角色、判断权限 | `RbacUserRoleService.getRoles` / `hasRole` / `hasPermission` |
| 运行时增角色/接口/绑定 | `RbacConfigAdminService.addRoleDefinition` / `addApi` / `bindRoleApi` |
| YAML 导入/导出 | `RbacConfigAdminService.importFromYaml` / `exportToYaml` |
| 白名单路径（代码） | 实现 `RbacExcludePathCustomizer.getExcludePaths()` |
| 自定义表字段映射 | 实现 `RbacFieldMappingCustomizer.customize(FieldMappingBuilder)` |
| 状态流转权限 | 实现 `RbacStateTransitionPolicy.allowedTransition`，业务在变更前调用 |
| 标注接口所需角色 | `@RbacController`、`@RbacMethod` |
| 枚举同步角色 | 枚举常量上 `@RbacRole(value, name)`，配置 `rbac.auto.role-enum-class` |

---

*文档版本与框架版本一致：1.0.0。*
