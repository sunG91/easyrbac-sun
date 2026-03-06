# EasyRBAC Sun

一个基于 Spring Boot 3.5+ 的 RBAC 权限框架，引入 Starter、配好数据源和角色就能用。支持自研 Token / JWT、可选 Redis 缓存、YAML 导入导出。

- **API 文档**：[API.md](API.md)
- **YAML 配置**：[yaml.md](yaml.md)

---

## 引入依赖

已发布到 Maven Central，直接加依赖即可：

```xml
<dependency>
    <groupId>io.github.sung91</groupId>
    <artifactId>easyrbac-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 快速开始

### 1. 配置数据源和角色

配好 `spring.datasource`，角色可以用 YAML 或枚举。

**YAML 方式：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password

rbac:
  enabled: true
  auto:
    role-mapping:
      10000: 超级管理员
      10001: 普通用户
    auto-create-table: true
    auto-sync-data: true
```

**枚举方式：** 在枚举上标 `@RbacRole`，配置 `role-enum-class` 即可，详见 [yaml.md](yaml.md)。

### 2. 在 Controller 上标权限

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

    @PostMapping("/export")
    @RbacMethod({"10000", "10002"})  // 任一角色即可
    public Result export() {
        return Result.success();
    }
}
```

### 3. 开启校验（可选）

```yaml
rbac:
  check:
    enabled: true
    type: internal
    internal:
      header: Authorization
      prefix: Bearer
      expire-seconds: 7200
      secret: your-32-chars-secret
    exclude-paths:
      - /login
      - /public/**
```

登录后调用 `RbacTokenIssuerService.issueToken(userId)` 签发 Token，请求时带上 `Authorization: Bearer <token>` 即可。

### 4. 操作用户角色

```java
@Autowired
private RbacUserRoleService rbacUserRoleService;

rbacUserRoleService.addRole("userId", "10000");
List<String> roles = rbacUserRoleService.getRoles("userId");
boolean ok = rbacUserRoleService.hasPermission("userId", "/user/list");
```

---

## 功能概览

- **自动建表**：四张表（角色、接口、角色-接口、用户-角色），支持自定义表名和字段
- **启动同步**：从 YAML 或枚举同步角色，从 `@RbacController` / `@RbacMethod` 扫描接口并同步
- **校验方式**：internal（自研 HMAC Token）或 jwt
- **Redis 缓存**：可选，启用后权限校验优先走缓存
- **运行时管理**：`RbacConfigAdminService` 支持 YAML 导入导出、动态增删角色和接口

更多配置见 [yaml.md](yaml.md)，扩展点见 [API.md](API.md)。

---

## 技术栈

- Java 17+
- Spring Boot 3.5.0+
- Maven 多模块

---

## 作者

| 项目 | 链接 |
|------|------|
| 作者 | SUNRUI |
| 邮箱 | sunr20050503@163.com |
| GitHub | [sunG91](https://github.com/sunG91) |
| Gitee | [xh_888](https://gitee.com/xh_888) |

- GitHub 仓库：<https://github.com/sunG91/easyrbac-sun>
- Gitee 仓库：<https://gitee.com/xh_888/easyrbac-sun>

---

## 许可证

[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0)
