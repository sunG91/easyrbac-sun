package com.sun.easyrbac.constant;

/**
 * RBAC 框架全局常量。
 * 便于 2.0/3.0 扩展时统一错误码与默认配置。
 *
 * @author SUNRUI
 * @see <a href="https://github.com/sunG91">GitHub</a>
 * @see <a href="https://gitee.com/xh_888">Gitee</a>
 */
public final class RbacConstants {

    private RbacConstants() {}

    /** 当前框架版本，用于日志与兼容性判断 */
    public static final String VERSION = "1.0.0";

    // ---------- 错误码（便于文档与排查） ----------
    /** 配置错误：缺少必配项 */
    public static final String ERR_CONFIG_REQUIRED = "RBAC_1001";
    /** 配置错误：表/字段映射缺少 id 或 path */
    public static final String ERR_FIELD_MAPPING = "RBAC_1002";
    /** 数据库错误：建表或同步失败 */
    public static final String ERR_DB_OPERATION = "RBAC_2001";
    /** 校验失败：无权限 */
    public static final String ERR_ACCESS_DENIED = "RBAC_3001";
    /** 校验失败：Token 无效或过期 */
    public static final String ERR_TOKEN_INVALID = "RBAC_3002";

    // ---------- 默认表名（可被配置覆盖） ----------
    public static final String DEFAULT_TABLE_ROLE = "rbac_role";
    public static final String DEFAULT_TABLE_API = "rbac_api";
    public static final String DEFAULT_TABLE_ROLE_API = "rbac_role_api";
    public static final String DEFAULT_TABLE_USER_ROLE = "rbac_user_role";

    // ---------- 默认列名（可被 field-mapping 覆盖） ----------
    public static final String DEFAULT_COLUMN_ID = "id";
    public static final String DEFAULT_COLUMN_PATH = "path";
    public static final String DEFAULT_COLUMN_ROLE_CODE = "role_code";
    public static final String DEFAULT_COLUMN_ROLE_NAME = "role_name";
    public static final String DEFAULT_COLUMN_API_ID = "api_id";
    public static final String DEFAULT_COLUMN_ROLE_ID = "role_id";
    public static final String DEFAULT_COLUMN_USER_ID = "user_id";
    public static final String DEFAULT_COLUMN_CREATED_AT = "created_at";

    /** 权限 path 生成格式：小写+下划线 */
    public static final String API_PATH_FORMAT_LOWERCASE_UNDERSCORE = "lowercase_underscore";
    /** 权限 path 生成格式：驼峰 */
    public static final String API_PATH_FORMAT_CAMEL_CASE = "camelCase";
    /** 权限 path 生成格式：请求路径 */
    public static final String API_PATH_FORMAT_PATH = "path";
}
