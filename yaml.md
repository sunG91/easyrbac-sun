## EasyRBAC 1.0.0 YAML 配置说明

本文说明 `application.yml` / `application.properties` 中所有以 `rbac.` 开头的配置项，包含：

- **整体结构**：`rbac.enabled`、`rbac.auto.*`、`rbac.check.*`、`rbac.cache.*`
- **默认值与含义**：每个字段的默认值、作用
- **可重写项与示例**：哪些可以在 YAML 中覆盖，如何写，给出推荐示例

所有配置类集中在 `com.sun.easyrbac.config.RbacProperties`，其上有 `@ConfigurationProperties(prefix = "rbac")`。

---

### 1. 总体结构与最小示例

**整体结构**：

```yaml
rbac:
  enabled: true
  auto:             # 自动建表与同步、表名/字段等
    ...
  check:            # Token 校验、拦截、多种实现
    ...
  cache:            # 权限/角色缓存 TTL
    ...
```

**最小可用示例（internal 自研 Token，使用应用自己的数据源）**：

```yaml
rbac:
  enabled: true
  auto:
    auto-create-table: true        # 默认 true，可按需关闭
    auto-sync-data: true           # 默认 true，同步角色/接口/绑定
  check:
    enabled: true
    type: internal                 # internal（默认）或 jwt
    internal:
      header: Authorization
      prefix: Bearer
      expire-seconds: 7200
      # user-identifier-type 仅作说明，签发时可传任意类型（String/Long/Integer 等）
      user-identifier-type: string
```

以上所有字段都可以通过 YAML **重写覆盖默认值**。

---

### 2. `rbac.enabled` —— 框架总开关

- **路径**：`rbac.enabled`
- **类型**：`boolean`
- **默认值**：`true`
- **含义**：是否启用整个 RBAC 框架。为 false 时不创建表、不注册拦截器和过滤器，相当于关闭所有功能。

**重写示例**（仅在某些环境关闭，如本地集成测试）：

```yaml
rbac:
  enabled: false
```

---

### 3. `rbac.auto.*` —— 自动建表、同步与表结构配置

对应 `RbacProperties.Auto` 与 `RbacProperties.Db`。

#### 3.1 数据库连接 `rbac.auto.db.*`

仅在需要**使用独立连接**进行建表/同步时配置；如果你希望使用应用自己的 `DataSource`，可以留空，框架会复用默认数据源。

- `rbac.auto.db.url`：JDBC URL，默认空字符串
- `rbac.auto.db.username`：用户名，默认空字符串
- `rbac.auto.db.password`：密码，默认空字符串

**重写示例：使用单独的 RBAC 数据库（可选）**

```yaml
rbac:
  auto:
    db:
      url: jdbc:mysql://127.0.0.1:3306/rbac_demo?useSSL=false&characterEncoding=UTF-8
      username: root
      password: 123456
```

#### 3.2 自动建表与同步行为

- `rbac.auto.auto-create-table`（boolean，默认 `true`）  
  是否在启动时自动创建/更新 RBAC 相关表（role、api、role_api、user_role）。

- `rbac.auto.auto-sync-data`（boolean，默认 `true`）  
  是否在启动时根据注解、role-mapping、role-enum-class 同步角色/接口/角色-接口数据。

- `rbac.auto.sync-role-table`（boolean，默认 `true`）  
- `rbac.auto.sync-api-table`（boolean，默认 `true`）  
- `rbac.auto.sync-role-api-table`（boolean，默认 `true`）  
  分别控制是否同步对应表。一般保持默认即可。

- `rbac.auto.sync-async`（boolean，默认 `false`）  
  是否**异步**执行启动同步，减少对启动耗时的影响。开启后，同步在后台线程执行。

**重写示例：线上环境关闭自动建表，只保留配置同步**

```yaml
rbac:
  auto:
    auto-create-table: false
    auto-sync-data: true
```

#### 3.3 扫描与表名

- `rbac.auto.scan-packages`（String，默认 `""`）  
  用于扫描 `@RbacController`、`@RbacMethod` 的 Controller 包，多个包用逗号分隔；为空则扫描所有 Controller 包。

- 表名：
  - `rbac.auto.role-table`（默认 `"rbac_role"`）
  - `rbac.auto.api-table`（默认 `"rbac_api"`）
  - `rbac.auto.role-api-table`（默认 `"rbac_role_api"`）
  - `rbac.auto.user-role-table`（默认 `"rbac_user_role"`）

**重写示例：与现有表名对齐**

```yaml
rbac:
  auto:
    scan-packages: com.demo.user,com.demo.order
    role-table: sys_role
    api-table: sys_api
    role-api-table: sys_role_api
    user-role-table: sys_user_role
```

#### 3.4 path 生成方式与注解模式

- `rbac.auto.api-path-format`（String，默认 `"lowercase_underscore"`）  
  - `"lowercase_underscore"`：默认，将 Controller+方法名转换为下划线风格  
  - `"camelCase"`：驼峰风格  
  - `"path"`：直接使用接口请求 path 作为权限 ID

- `rbac.auto.annotation-mode`（String，默认 `"id"`）  
  - `"id"`：使用注解上的 `id` / `value` 作为权限 ID  
  - `"path"`：使用注解或接口 path 作为权限地址

**重写示例：以接口 path 作为权限 ID**

```yaml
rbac:
  auto:
    api-path-format: path
    annotation-mode: path
```

#### 3.5 角色来源：`role-mapping` 与 `role-enum-class`

- `rbac.auto.role-mapping`（Map<String, String>，默认空）  
  角色编码 → 角色名称。启动同步时会将这里的映射写入角色表。

- `rbac.auto.role-enum-class`（String，默认空）  
  指定一个枚举类，全限定名（如 `com.demo.auth.AppRole`）。  
  若配置该项且枚举常量上有 `@RbacRole`，则可自动从枚举同步角色表，此时可以不配置 `role-mapping`。

**重写示例：YAML 中直接配置角色**

```yaml
rbac:
  auto:
    role-mapping:
      "10000": 超级管理员
      "10001": 普通用户
```

**重写示例：使用枚举同步角色**

```yaml
rbac:
  auto:
    role-enum-class: com.demo.auth.AppRole
```

> **可重写点**：如果既配置了 `role-mapping` 又配置了 `role-enum-class`，两者会合并（去重），最终写入同一角色表。

#### 3.6 字段映射 `rbac.auto.field-mapping`

- 路径：`rbac.auto.field-mapping`
- 类型：Map<String, String>
- 默认值：空 Map（但内部要求至少为 `id`、`path` 配置列名，否则启动时抛错）
- 作用：将逻辑字段名映射为数据库列名，例如：
  - `id`：主键列名
  - `path`：接口 path 列名
  - 其他自定义字段

**重写示例：自定义列名**

```yaml
rbac:
  auto:
    field-mapping:
      id: id
      path: api_path
      role_code: role_code
```

> **可重写点**：你也可以通过实现 `RbacFieldMappingCustomizer` 在代码中补充/修改字段映射；YAML 与代码的映射会合并，代码优先。

---

### 4. `rbac.check.*` —— Token 校验与拦截配置

对应 `RbacProperties.Check`、`Internal`、`Jwt`、`Redis`。

#### 4.1 核心开关与模式

- `rbac.check.enabled`（boolean，默认 `false`）  
  是否启用权限校验与拦截。为 false 时不解析 Token、不做权限判断。

- `rbac.check.type`（String，默认 `"internal"`）  
  - `"internal"`：使用框架自研 Token（HMAC + `userId:expireAtMillis`）  
  - `"jwt"`：使用 JWT 校验（框架只负责校验，不负责签发）

- `rbac.check.exclude-paths`（List<String>，默认空）  
  不参与权限校验的路径，支持 Ant 风格，如 `/login`、`/public/**`。

- `rbac.check.intercept-mode`（String，默认 `"all"`）  
  - `"all"`：拦截所有 HTTP 请求（除 `exclude-paths` 与自定义排除以外）  
  - `"annotated"`：仅拦截标注了 `@RbacController` / `@RbacMethod` 的接口

**重写示例：只拦截带注解的接口，并放行登录/开放接口**

```yaml
rbac:
  check:
    enabled: true
    type: internal
    intercept-mode: annotated
    exclude-paths:
      - /login
      - /public/**
```

> **可重写点**：除了 YAML 中的 `exclude-paths`，还可以实现 `RbacExcludePathCustomizer` 在代码中动态追加放行路径。

#### 4.2 internal 自研 Token：`rbac.check.internal.*`

对应 `RbacProperties.Internal`。

- `rbac.check.internal.header`（String，默认 `"Authorization"`）  
  请求头名。

- `rbac.check.internal.prefix`（String，默认 `"Bearer"`）  
  Token 前缀，如 `Bearer`。

- `rbac.check.internal.expire-seconds`（long，默认 `7200`）  
  Token 过期时间（秒）。内部统一按毫秒存储。

- `rbac.check.internal.secret`（String，默认空字符串）  
  HMAC 密钥；为空时启动时会生成随机密钥（仅适合开发环境），重启后签发的旧 Token 会失效。

- `rbac.check.internal.user-identifier-type`（String，默认 `"string"`）  
  用户标识类型约定：`string` / `long` / `integer`。  
  仅作为**文档与约定**存在，实际签发时 `RbacTokenIssuerService.issueToken(Object userId)` 接受任意类型（String/Long/Integer 等），内部会统一转为字符串写入。

**重写示例：生产环境固定密钥 + Long 类型 userId**

```yaml
rbac:
  check:
    enabled: true
    type: internal
    internal:
      header: Authorization
      prefix: Bearer
      expire-seconds: 86400
      secret: change-me-to-32-chars-at-least
      user-identifier-type: long
```

#### 4.3 JWT 模式：`rbac.check.jwt.*`

对应 `RbacProperties.Jwt`。仅在 `rbac.check.type=jwt` 时生效。

- `rbac.check.jwt.header`（String，默认 `"Authorization"`）  
- `rbac.check.jwt.prefix`（String，默认 `"Bearer"`）  
- `rbac.check.jwt.secret`（String，默认空字符串）  
  - 用于校验 JWT 签名的密钥（或公钥相关配置，视你的实现而定）。  
  - 框架默认只校验签名和过期时间，不负责签发。

**重写示例：使用 JWT 校验**

```yaml
rbac:
  check:
    enabled: true
    type: jwt
    jwt:
      header: Authorization
      prefix: Bearer
      secret: your-jwt-secret-or-public-key
```

> **可重写点**：JWT 的具体载荷（sub、roles 等）由业务侧自行定义；框架只负责从 JWT 中解析出用户标识并做角色/权限判断。  
> 此外，无论 internal 还是 jwt 模式，`RbacTokenValidator.validate` 都支持直接传入带前缀的字符串（如 `"Bearer xxx"`），框架会自动剥离前缀，业务无需手动处理。

#### 4.4 Redis 配置：`rbac.check.redis.*`

对应 `RbacProperties.Redis`。用于 RBAC 内部的**登录态**与**角色缓存**等使用的 Redis 连接。

字段：

- `rbac.check.redis.host`（String，默认 `"localhost"`）
- `rbac.check.redis.port`（int，默认 `6379`）
- `rbac.check.redis.password`（String，默认空）
- `rbac.check.redis.database`（int，默认 `0`）
- `rbac.check.redis.expire-time`（long，默认 `7200`）  
  登录态等在 Redis 中的过期时间（秒）。当使用 `RbacTokenIssuerService.issueTokenAndCache` 时，会以此为 TTL 写入登录态；同时会加载用户当前角色列表并写入角色缓存，后续权限校验优先走 Redis。
- `rbac.check.redis.key-by`（String，默认 `"user_id"`）  
  key 构成方式，可按业务含义自定义：  
  - `"user_id"`：登录态 key 形如 `key-prefix + userId`（默认推荐）  
  - `"token"`：登录态 key 形如 `key-prefix + token`
- `rbac.check.redis.key-prefix`（String，默认 `"rbac:login:"`）

**重写示例：RBAC 使用独立 Redis 实例（默认 db=0，可按需自定义）**

```yaml
rbac:
  check:
    redis:
      host: 192.168.1.100
      port: 6379
      password: 123456
      database: 2
      expire-time: 7200
      key-by: user_id
      key-prefix: rbac:login:
```

> **可重写点**：当应用中**没有**自定义的 `StringRedisTemplate` 时，配置了 `rbac.check.redis` 后，框架会通过 `RbacRedisConnectionConfiguration` 使用这些参数创建一套独立的 Redis 连接，不会影响你原有的 Redis 使用。
> 当 `rbac.check.enabled=true` 且启用了 Redis 时，请求侧 Token 虽然通过签名/过期校验，但若 Redis 中不存在对应登录态 key（根据 `key-by` 决定是按 userId 还是 token 查），框架会直接返回「未登录」（HTTP 401，错误码 `RBAC_3002`），即视为登录态已失效。

---

### 5. `rbac.cache.*` —— 权限/角色缓存

对应 `RbacProperties.Cache`。

- `rbac.cache.ttl`（long，默认 `300`，单位：秒）  
  角色/权限缓存的 TTL。仅在启用了 Redis 角色缓存时生效。

**重写示例：将角色缓存 TTL 提高到 10 分钟**

```yaml
rbac:
  cache:
    ttl: 600
```

---

### 6. YAML 与代码扩展点的关系

除了以上 YAML 配置外，框架还提供若干**可通过代码扩展的接口**，它们与 YAML 的关系如下：

- **白名单路径**  
  - YAML：`rbac.check.exclude-paths`  
  - 代码：实现 `RbacExcludePathCustomizer` 追加动态放行路径

- **表字段映射**  
  - YAML：`rbac.auto.field-mapping`  
  - 代码：实现 `RbacFieldMappingCustomizer` 自定义映射，优先级高于 YAML

- **角色来源**  
  - YAML：`rbac.auto.role-mapping`  
  - 代码：在枚举上使用 `@RbacRole` 并配置 `rbac.auto.role-enum-class`

> 建议优先使用 YAML 做环境差异配置（如表名、Redis 连接、TTL、是否启用校验等），使用代码扩展点处理**业务逻辑相关**的差异（如状态流转策略、自定义白名单、复杂字段映射）。

---

### 7. 典型代码重写与集成示例

本节给出几段完整的代码示例，说明如何在**代码层面重写或使用 YAML 配置**。

#### 7.1 按环境动态追加白名单路径

```java
import com.sun.easyrbac.ext.RbacExcludePathCustomizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class MyExcludePathCustomizer implements RbacExcludePathCustomizer {
    @Override
    public List<String> getExcludePaths() {
        // 在 application.yml 中的 exclude-paths 基础上，追加一些路径
        return Arrays.asList("/actuator/**", "/health");
    }
}
```

#### 7.2 通过代码重写表字段映射

```java
import com.sun.easyrbac.ext.FieldMappingBuilder;
import com.sun.easyrbac.ext.RbacFieldMappingCustomizer;
import org.springframework.stereotype.Component;

@Component
public class MyFieldMappingCustomizer implements RbacFieldMappingCustomizer {
    @Override
    public void customize(FieldMappingBuilder builder) {
        builder
            .mapIdTo("id")                 // 显式指定主键列（可覆盖 YAML）
            .mapPathTo("api_path")         // 显式指定 path 列
            .addMapping("role_code", "role_code"); // 追加其他列映射
    }
}
```

#### 7.3 使用枚举声明角色并同步到数据库

```java
import com.sun.easyrbac.annotation.RbacRole;

public enum AppRole {
    @RbacRole(value = "10000", name = "超级管理员")
    SUPER_ADMIN,

    @RbacRole(value = "10001", name = "普通用户")
    NORMAL_USER
}
```

配合 YAML：

```yaml
rbac:
  auto:
    role-enum-class: com.demo.auth.AppRole
```

> 启动时，框架会从该枚举中读取 `@RbacRole`，并与 `rbac.auto.role-mapping` 一起同步到角色表。

#### 7.5 在 Controller 注解中使用角色枚举

在使用枚举同步角色表的基础上，可以通过 `@RbacController` / `@RbacMethod` 的 `roleEnums` 字段，将这些枚举类中带 `@RbacRole` 的常量作为接口所需角色，无需在注解里重复写角色编码：

```java
import com.sun.easyrbac.annotation.RbacController;
import com.sun.easyrbac.annotation.RbacMethod;

@RestController
@RequestMapping("/user")
@RbacController(roleEnums = {AppRole.class})          // 类级默认角色来自 AppRole
public class UserController {

    @GetMapping("/list")
    @RbacMethod(roleEnums = {AppRole.class})          // 方法级角色也来自 AppRole
    public Result list() {
        return Result.success();
    }
}
```

> 说明：`roleEnums` 是对原有 `value` / `id` / `path` 的补充，三者会合并后参与权限判断；现有基于字符串编码的写法保持完全兼容。

#### 7.4 在业务代码中直接读取 RbacProperties

```java
import com.sun.easyrbac.config.RbacProperties;
import org.springframework.stereotype.Component;

@Component
public class RbacConfigPrinter {

    private final RbacProperties properties;

    public RbacConfigPrinter(RbacProperties properties) {
        this.properties = properties;
    }

    public void print() {
        System.out.println("RBAC enabled = " + properties.isEnabled());
        System.out.println("Check type = " + properties.getCheck().getType());
        System.out.println("Role table = " + properties.getAuto().getRoleTable());
    }
}
```

> 通过注入 `RbacProperties`，可以在代码中方便地读取 YAML 配置，用于日志、调试或二次封装；配置本身仍推荐通过 YAML/环境变量进行修改。

---

### 8. 一个接口多个角色/权限的 YAML 示例

在模型设计上，一个接口（`RbacApi` 一条记录）对应一个权限 ID/path，**可以绑定到多个角色**。  
接口的权限 ID/path 相同，只要用户拥有其中任意一个绑定到该接口的角色，即可通过校验（OR 关系）。

配合 `RbacConfigAdminService.importFromYaml` / `exportToYaml` 使用时，可以在 YAML 中这样声明：

```yaml
roles:
  - code: "10000"
    name: "管理员"
  - code: "10001"
    name: "运营"

apis:
  - path: "/user/list"
    method: "GET"
    controller: "UserController"

role-apis:
  # 管理员可以访问 /user/list
  - role-code: "10000"
    api-path: "/user/list"
  # 运营也可以访问 /user/list
  - role-code: "10001"
    api-path: "/user/list"
```

含义：

- `/user/list` 这个接口只对应一个权限点（同一个 `path`），但同时出现在两条 `role-apis` 映射中；
- 只要当前登录用户拥有角色 `10000` 或 `10001`，访问 `/user/list` 时就会被认为有权限；
- 若需要新增第三个角色访问该接口，只需再追加一条 `role-apis` 记录绑定到相同的 `api-path` 即可。

