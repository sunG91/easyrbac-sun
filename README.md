# EasyRBAC Sun

基于 **Spring Boot 3.5+** 的 RBAC 权限框架，1.0.0 单体应用实现。引入 Starter 后配置数据库与角色列表即可使用，支持自研 Token / JWT、无 Redis 或有 Redis 缓存策略、YAML 导入导出与运行时角色/接口管理。

- **API 文档**：[API.md](API.md) — 所有对外方法、扩展点、注解、模型与常量的调用方式、逻辑、参数与注释。

---

## 作者与仓库

| 项目 | 链接 |
|------|------|
| **作者** | SUNRUI |
| **邮箱** | sunr20050503@163.com |
| **GitHub** | [https://github.com/sunG91](https://github.com/sunG91) |
| **Gitee** | [https://gitee.com/xh_888](https://gitee.com/xh_888) |

- **GitHub 仓库**：[https://github.com/sunG91/easyrbac-sun](https://github.com/sunG91/easyrbac-sun)
- **Gitee 仓库**：[https://gitee.com/xh_888](https://gitee.com/xh_888) 下同名项目

---

## 技术栈与版本

- **Java**：17+
- **Spring Boot**：3.5.0+（最低要求）
- **包结构**：`com.sun.easyrbac`
- **构建**：Maven 多模块

---

## 模块说明

| 模块 | 说明 |
|------|------|
| **easyrbac-common** | 公共常量、异常、模型（如错误码、RbacException、RoleMappingItem） |
| **easyrbac-core** | 领域与仓储接口（RbacRole/RbacApi、RbacRoleRepository 等），无 Spring 强依赖 |
| **easyrbac-spring-boot-starter** | 1.0.0 单体 Starter：自动配置、建表、同步、校验、缓存、YAML 导入导出 |

业务项目只需引入 **easyrbac-spring-boot-starter** 即可。

---

## 1.0.0 功能简介

- **最少必配**：配置数据源（`spring.datasource`）与角色来源二选一：**YAML `rbac.auto.role-mapping`** 或 **枚举 + `rbac.auto.role-enum-class`**，即可自动建表并同步角色/接口/角色-接口。
- **四张表**：角色表、接口表、角色-接口关联表、用户-角色关联表；支持自定义表名与字段映射（YAML `field-mapping` 或代码 `RbacFieldMappingCustomizer` + `FieldMappingBuilder`）。
- **启动同步**：角色来自 `role-mapping`（YAML）或 `role-enum-class`（枚举）；接口从带 `@RbacController` / `@RbacMethod` 的 Controller 扫描并增量同步，不删用户已有数据；可配置 `sync-role-table` / `sync-api-table` / `sync-role-api-table`、`sync-async`、`scan-packages`。
- **权限 path 生成**：`api-path-format` 支持 `lowercase_underscore`（默认）、`camelCase`、`path`。
- **校验方式**：  
  - **internal**：自研 Token（HMAC + 过期），配置 `rbac.check.type: internal`。  
  - **jwt**：标准 JWT 解析与校验（payload 取 `sub`/`exp`），配置 `rbac.check.type: jwt`。  
  - 支持 `exclude-paths`（YAML）与 `RbacExcludePathCustomizer`（代码白名单）；`intercept-mode` 支持 `all` / `annotated`（仅拦截带 RBAC 注解的接口）。
- **业务层 API**：`RbacUserRoleService` 提供 `addRole` / `addRoles` / `removeRole` / `removeRoles` / `getRoles` / `hasRole` / `hasPermission`；无 Redis 时直接查库，有 Redis 时走角色缓存（key 前缀 `rbac:roles:`，add/remove 后自动失效）。
- **运行时管理**：`RbacConfigAdminService` 提供 `addRoleDefinition`、`addApi`、`bindRoleApi`、`importFromYaml(InputStream)`、`exportToYaml(OutputStream)`，用于生产环境动态角色/接口与 YAML 导入导出。
- **扩展点**：`RbacExcludePathCustomizer`、`RbacFieldMappingCustomizer`、`FieldMappingBuilder`、`RbacTokenValidator`（internal/jwt 实现）、`RbacStateTransitionPolicy`（状态流转，业务实现后自行调用）。
- **错误码**：统一 `RbacException`，错误码见 `RbacConstants`（如 `ERR_ACCESS_DENIED`、`ERR_TOKEN_INVALID` 等）。

---

## 快速开始

### 1. 引入依赖

当前 1.0.0 的 jar 托管在 **Gitee** 与 **GitHub** 的独立 JAR 仓库，使用前需先将 jar 安装到本地 Maven 仓库，再在项目中声明依赖。

**第一步：下载 jar 并安装到本地 Maven 仓库**

- **GitHub**  
  - 仓库内文件：[easyrbac-spring-boot-starter-1.0.0.jar](https://github.com/sunG91/easyrbac-sun-jar/blob/main/easyrbac-spring-boot-starter-1.0.0.jar)  
  - 直接下载：`https://github.com/sunG91/easyrbac-sun-jar/raw/main/easyrbac-spring-boot-starter-1.0.0.jar`
- **Gitee（推荐国内）**  
  - 仓库内文件：[easyrbac-spring-boot-starter-1.0.0.jar](https://gitee.com/xh_888/easyrbac-sun-jar/blob/master/easyrbac-spring-boot-starter-1.0.0.jar)  
  - 直接下载：`https://gitee.com/xh_888/easyrbac-sun-jar/raw/master/easyrbac-spring-boot-starter-1.0.0.jar`

下载后执行（将路径改为你本地的 jar 路径）：

```bash
mvn install:install-file -Dfile=easyrbac-spring-boot-starter-1.0.0.jar -DgroupId=com.sun.easyrbac -DartifactId=easyrbac-spring-boot-starter -Dversion=1.0.0 -Dpackaging=jar
```

**第二步：在项目中添加 Maven 依赖**

```xml
<dependency>
    <groupId>com.sun.easyrbac</groupId>
    <artifactId>easyrbac-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置数据源与角色列表

本 Starter 使用应用主数据源（`JdbcTemplate`），请在 `application.yml` 中配置 **spring.datasource**，角色列表等 RBAC 配置使用 **rbac** 前缀。

**方式一：YAML 配置 role-mapping**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

rbac:
  enabled: true
  auto:
    role-mapping:
      10000: 超级管理员
      10001: 普通用户
      10002: 运营人员
    auto-create-table: true
    auto-sync-data: true
```

**方式二：使用枚举（可不配 role-mapping）**

定义枚举并标注 `@RbacRole(value="角色编码", name="角色名")`，`name` 为空时使用枚举常量名。在 YAML 中只配置 `role-enum-class` 为枚举全限定类名即可，无需再写 `role-mapping`：

```java
public enum AppRole {
    @RbacRole(value = "10000", name = "超级管理员")
    SUPER_ADMIN,
    @RbacRole(value = "10001", name = "普通用户")
    NORMAL_USER,
    @RbacRole(value = "10002", name = "运营人员")
    OPERATOR
}
```

```yaml
rbac:
  enabled: true
  auto:
    role-enum-class: com.example.yourpackage.AppRole   # 枚举全限定名
    auto-create-table: true
    auto-sync-data: true
```

启动后框架会自动建表并同步角色与接口（从枚举或 role-mapping + 带 RBAC 注解的 Controller 扫描）。

### 3. Controller 上标注权限

```java
@RestController
@RequestMapping("/user")
@RbacController("10001")
public class UserController {

    @GetMapping("/list")
    @RbacMethod("10000")
    public Result list() {
        return Result.success();
    }

    @PostMapping("/add")
    public Result add(User user) {
        return Result.success();
    }
}
```

### 4. 开启校验（可选）

需要请求时校验 Token 与角色时：

```yaml
rbac:
  check:
    enabled: true
    type: internal   # 或 jwt
    internal:
      header: Authorization
      prefix: Bearer
      expire-seconds: 7200
      secret: your-32-chars-secret
    exclude-paths:
      - /login
      - /public/**
```

**登录后签发 Token（推荐）**  
业务只需在「登录校验通过」后传入 userId，由框架生成与当前配置一致的 Token，无需自实现 HMAC 或关心过期时间、请求头前缀。`type=internal` 时注入 `RbacTokenIssuerService`，调用 `issueToken(userId)` 即可：

```java
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private RbacTokenIssuerService rbacTokenIssuerService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        // 1. 校验用户名密码（查库、BCrypt 等）
        User user = authService.authenticate(dto.getUsername(), dto.getPassword());
        if (user == null) return Result.fail(401, "用户名或密码错误");
        // 2. 由框架签发 Token（与 rbac.check 配置一致，internal 时直接可用）
        RbacTokenResult tokenResult = rbacTokenIssuerService.issueToken(user.getId());
        if (tokenResult == null) return Result.fail(500, "Token 签发失败"); // type=jwt 时需业务用 JWT 库自行签发
        // 3. 返回给前端，前端请求时带 Header: Authorization: Bearer <tokenResult.getToken()>
        return Result.success(LoginVO.builder()
                .token(tokenResult.getToken())
                .type(tokenResult.getType())
                .expireSeconds(tokenResult.getExpireSeconds())
                .userId(tokenResult.getUserId())
                .build());
    }
}
```

请求头携带 `Authorization: Bearer <token>`，Token 合法且用户具备接口所需角色之一即可通过。

### 5. Redis 角色缓存（可选）

启用后，权限校验会优先从 Redis 读用户角色，未命中再查库并回写缓存；`addRole`/`removeRole` 后会自动失效对应用户缓存。

**方式一：仅配置 rbac.check.redis（RBAC 独立连 Redis）**

1. 在项目中引入 Redis 依赖（若未引入）：
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```
2. 只配置 `rbac.check.redis`，**不要**配置 `spring.data.redis`，框架会用 `rbac.check.redis` 自动创建连接：
   ```yaml
   rbac:
     check:
       redis:
         host: localhost
         port: 6379
         database: 0
         password:          # 无密码留空
         expire-time: 7200  # 可选，缓存过期秒数
     cache:
       ttl: 300   # 角色缓存 TTL（秒），可选
   ```

**方式二：使用应用已有 Redis（spring.data.redis）**

若项目已配置 `spring.data.redis`，框架会直接使用应用已有的 Redis 连接，无需再配 `rbac.check.redis` 的 host/port；只需用 `rbac.check.redis.expire-time` 或 `rbac.cache.ttl` 调节角色缓存 TTL 即可。

### 6. 业务层操作用户与角色

```java
@Autowired
private RbacUserRoleService rbacUserRoleService;

// 为用户绑定角色
rbacUserRoleService.addRole("userId", "10000");
// 查询用户角色、判断权限
List<String> roles = rbacUserRoleService.getRoles("userId");
boolean ok = rbacUserRoleService.hasRole("userId", "10000");
```

---

## 配置项速查

| 配置项 | 说明 | 默认 |
|--------|------|------|
| `rbac.enabled` | 总开关 | true |
| `rbac.auto.role-mapping` | 角色编码 → 角色名称（YAML） | 与 role-enum-class 二选一 |
| `rbac.auto.role-enum-class` | 角色枚举全限定名（从枚举同步时可省略 role-mapping） | 可选 |
| `rbac.auto.auto-create-table` | 是否自动建表 | true |
| `rbac.auto.auto-sync-data` | 是否启动时同步 | true |
| `rbac.auto.sync-role-table` / `sync-api-table` / `sync-role-api-table` | 同步哪些表 | true |
| `rbac.auto.role-table` / `api-table` / `role-api-table` / `user-role-table` | 表名 | rbac_* |
| `rbac.auto.field-mapping` | 字段映射（id、path 必配） | 可选 |
| `rbac.auto.api-path-format` | lowercase_underscore / camelCase / path | lowercase_underscore |
| `rbac.auto.scan-packages` | 只扫描的包，逗号分隔 | 空=全扫描 |
| `rbac.auto.sync-async` | 是否异步同步 | false |
| `rbac.check.enabled` | 是否开启校验 | false |
| `rbac.check.type` | internal / jwt | internal |
| `rbac.check.exclude-paths` | 白名单路径（Ant 风格） | 空 |
| `rbac.check.intercept-mode` | all / annotated | all |
| `rbac.check.redis.host` / `port` / `database` / `password` | 角色缓存 Redis 连接（未配 spring.data.redis 时由框架用此建连） | host=localhost, port=6379 |
| `rbac.check.redis.expire-time` | 角色缓存过期秒数 | 7200 |
| `rbac.cache.ttl` | 缓存 TTL（秒） | 300 |

---

## 运行时 YAML 导入/导出

注入 `RbacConfigAdminService` 即可在管理接口中调用：

```java
@Autowired
private RbacConfigAdminService rbacConfigAdminService;

// 从 YAML 导入（幂等增量）
rbacConfigAdminService.importFromYaml(inputStream);

// 导出当前库中角色、接口、角色-接口为 YAML
rbacConfigAdminService.exportToYaml(outputStream);
```

导入 YAML 结构示例：

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

## 扩展点示例

- **接口白名单（代码）**：实现 `RbacExcludePathCustomizer`，返回需放行的路径列表。
- **表/字段映射（代码）**：实现 `RbacFieldMappingCustomizer`，在 `FieldMappingBuilder` 中配置 `mapIdTo` / `mapPathTo` 等。
- **状态流转**：实现 `RbacStateTransitionPolicy`，在业务状态变更前调用 `allowedTransition(fromState, toState, roleCode)`。

---

## 许可证

[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

## 后续发展方向

- **1.0.0（当前）**：单体应用内 RBAC，自动建表与同步、注解标注、internal/JWT 校验、无 Redis/有 Redis 角色缓存、YAML 导入导出与运行时角色/接口管理。
- **2.0.0（规划）**：面向微服务架构，支持权限中心、各服务与中心同步、网关/服务内统一校验等。
- **3.0.0（规划）**：企业级能力与可视化配置，如管理后台、权限/角色可视化配置、审计与报表等。

如有问题或建议，欢迎通过上述邮箱或 GitHub/Gitee 仓库联系。
1 ·