package com.sun.easyrbac.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC 配置属性，对应 application 中 rbac 前缀。
 * 便于 2.0/3.0 在此类或子配置中扩展新项。
 *
 * @author SUNRUI
 */
@ConfigurationProperties(prefix = "rbac")
public class RbacProperties {

    /** 是否启用 RBAC（总开关） */
    private boolean enabled = true;
    /** 自动建表与数据同步相关 */
    private Auto auto = new Auto();
    /** 校验相关 */
    private Check check = new Check();
    /** 缓存配置（权限/角色缓存 TTL 等） */
    private Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Auto getAuto() {
        return auto;
    }

    public void setAuto(Auto auto) {
        this.auto = auto;
    }

    public Check getCheck() {
        return check;
    }

    public void setCheck(Check check) {
        this.check = check;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /** 自动建表、同步、扫描等配置 */
    public static class Auto {
        private Db db = new Db();
        /** 角色编码 → 角色名称 */
        private Map<String, String> roleMapping = new LinkedHashMap<>();
        private boolean autoCreateTable = true;
        private boolean autoSyncData = true;
        private boolean syncRoleTable = true;
        private boolean syncApiTable = true;
        private boolean syncRoleApiTable = true;
        /** 扫描的 Controller 包，空则扫描全部 */
        private String scanPackages = "";
        /** 表名配置 */
        private String roleTable = "rbac_role";
        private String apiTable = "rbac_api";
        private String roleApiTable = "rbac_role_api";
        private String userRoleTable = "rbac_user_role";
        /** path 生成格式：lowercase_underscore / camelCase / path */
        private String apiPathFormat = "lowercase_underscore";
        /** 标注方式：id（默认）/ path */
        private String annotationMode = "id";
        /** 角色枚举类全限定名，可选 */
        private String roleEnumClass = "";
        /** 是否异步执行启动同步 */
        private boolean syncAsync = false;
        /** 字段映射：逻辑名 → 数据库列名（id、path 必配） */
        private Map<String, String> fieldMapping = new LinkedHashMap<>();

        public Db getDb() {
            return db;
        }

        public void setDb(Db db) {
            this.db = db;
        }

        public Map<String, String> getRoleMapping() {
            return roleMapping;
        }

        public void setRoleMapping(Map<String, String> roleMapping) {
            this.roleMapping = roleMapping;
        }

        public boolean isAutoCreateTable() {
            return autoCreateTable;
        }

        public void setAutoCreateTable(boolean autoCreateTable) {
            this.autoCreateTable = autoCreateTable;
        }

        public boolean isAutoSyncData() {
            return autoSyncData;
        }

        public void setAutoSyncData(boolean autoSyncData) {
            this.autoSyncData = autoSyncData;
        }

        public boolean isSyncRoleTable() {
            return syncRoleTable;
        }

        public void setSyncRoleTable(boolean syncRoleTable) {
            this.syncRoleTable = syncRoleTable;
        }

        public boolean isSyncApiTable() {
            return syncApiTable;
        }

        public void setSyncApiTable(boolean syncApiTable) {
            this.syncApiTable = syncApiTable;
        }

        public boolean isSyncRoleApiTable() {
            return syncRoleApiTable;
        }

        public void setSyncRoleApiTable(boolean syncRoleApiTable) {
            this.syncRoleApiTable = syncRoleApiTable;
        }

        public String getScanPackages() {
            return scanPackages;
        }

        public void setScanPackages(String scanPackages) {
            this.scanPackages = scanPackages != null ? scanPackages : "";
        }

        public String getRoleTable() {
            return roleTable;
        }

        public void setRoleTable(String roleTable) {
            this.roleTable = roleTable;
        }

        public String getApiTable() {
            return apiTable;
        }

        public void setApiTable(String apiTable) {
            this.apiTable = apiTable;
        }

        public String getRoleApiTable() {
            return roleApiTable;
        }

        public void setRoleApiTable(String roleApiTable) {
            this.roleApiTable = roleApiTable;
        }

        public String getUserRoleTable() {
            return userRoleTable;
        }

        public void setUserRoleTable(String userRoleTable) {
            this.userRoleTable = userRoleTable;
        }

        public String getApiPathFormat() {
            return apiPathFormat;
        }

        public void setApiPathFormat(String apiPathFormat) {
            this.apiPathFormat = apiPathFormat;
        }

        public String getAnnotationMode() {
            return annotationMode;
        }

        public void setAnnotationMode(String annotationMode) {
            this.annotationMode = annotationMode != null ? annotationMode : "id";
        }

        public String getRoleEnumClass() {
            return roleEnumClass;
        }

        public void setRoleEnumClass(String roleEnumClass) {
            this.roleEnumClass = roleEnumClass != null ? roleEnumClass : "";
        }

        public boolean isSyncAsync() {
            return syncAsync;
        }

        public void setSyncAsync(boolean syncAsync) {
            this.syncAsync = syncAsync;
        }

        public Map<String, String> getFieldMapping() {
            return fieldMapping;
        }

        public void setFieldMapping(Map<String, String> fieldMapping) {
            this.fieldMapping = fieldMapping != null ? fieldMapping : new LinkedHashMap<>();
        }
    }

    /** 数据库连接（用于自动建表与同步） */
    public static class Db {
        private String url = "";
        private String username = "";
        private String password = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url != null ? url : "";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username != null ? username : "";
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password != null ? password : "";
        }
    }

    /** 校验配置：1.0 支持 internal（框架自研）与 jwt 两种类型 */
    public static class Check {
        private boolean enabled = false;
        /** internal：框架自研 Token；jwt：标准 JWT */
        private String type = "internal";
        /** 内部实现（internal）配置 */
        private Internal internal = new Internal();
        /** JWT 配置（type=jwt 时生效） */
        private Jwt jwt = new Jwt();
        /** 放开的接口路径（不校验），支持 Ant 风格 */
        private List<String> excludePaths = new ArrayList<>();
        /** 拦截模式：all（默认）/ annotated（仅拦截已标注接口） */
        private String interceptMode = "all";
        /** Redis 配置（1.0 可选，用于登录态等） */
        private Redis redis = new Redis();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Internal getInternal() {
            return internal;
        }

        public void setInternal(Internal internal) {
            this.internal = internal;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
        }

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths != null ? excludePaths : new ArrayList<>();
        }

        public String getInterceptMode() {
            return interceptMode;
        }

        public void setInterceptMode(String interceptMode) {
            this.interceptMode = interceptMode != null ? interceptMode : "all";
        }

        public Redis getRedis() {
            return redis;
        }

        public void setRedis(Redis redis) {
            this.redis = redis;
        }
    }

    /** Redis 配置（1.0 可选） */
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        private long expireTime = 7200;
        private String keyBy = "user_id";
        private String keyPrefix = "rbac:login:";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host != null ? host : "localhost"; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password != null ? password : ""; }
        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        public String getKeyBy() { return keyBy; }
        public void setKeyBy(String keyBy) { this.keyBy = keyBy != null ? keyBy : "user_id"; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix != null ? keyPrefix : "rbac:login:"; }
    }

    /** 缓存配置 */
    public static class Cache {
        /** 权限/角色缓存 TTL（秒） */
        private long ttl = 300;

        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }
    }

    /** 框架自研 Token 配置（type=internal 时使用） */
    public static class Internal {
        /** 请求头名，如 Authorization */
        private String header = "Authorization";
        /** 前缀，如 Bearer */
        private String prefix = "Bearer";
        /** 过期时间（秒） */
        private long expireSeconds = 7200;
        /** 密钥（至少 32 字符），不配则启动时生成随机密钥（仅开发用） */
        private String secret = "";
        /** 用户标识类型：string / long / integer，仅文档与序列化约定，签发时传入任意类型均会转为字符串 */
        private String userIdentifierType = "string";

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header != null ? header : "Authorization";
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix != null ? prefix : "Bearer";
        }

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret != null ? secret : "";
        }

        public String getUserIdentifierType() {
            return userIdentifierType;
        }

        public void setUserIdentifierType(String userIdentifierType) {
            this.userIdentifierType = userIdentifierType != null ? userIdentifierType : "string";
        }
    }

    /** JWT 校验配置（type=jwt 时使用） */
    public static class Jwt {
        /** 请求头名 */
        private String header = "Authorization";
        /** 前缀，如 Bearer */
        private String prefix = "Bearer";
        /** 密钥（至少 256 位），填 no 则启动时随机生成（仅开发用） */
        private String secret = "";

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header != null ? header : "Authorization";
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix != null ? prefix : "Bearer";
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret != null ? secret : "";
        }
    }
}
