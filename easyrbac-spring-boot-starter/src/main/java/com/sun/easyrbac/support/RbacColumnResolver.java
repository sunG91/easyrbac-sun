package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;

import java.util.Map;

/**
 * 解析表字段映射：逻辑名 → 数据库列名。
 * 未配置时使用默认列名，便于建表与 JDBC 仓储统一。
 *
 * @author SUNRUI
 */
public class RbacColumnResolver {

    private final Map<String, String> mapping;

    public RbacColumnResolver(RbacProperties properties) {
        this.mapping = properties.getAuto().getFieldMapping();
    }

    public String getId() {
        return get("id", RbacConstants.DEFAULT_COLUMN_ID);
    }

    public String getPath() {
        return get("path", RbacConstants.DEFAULT_COLUMN_PATH);
    }

    public String getRoleCode() {
        return get("role-code", RbacConstants.DEFAULT_COLUMN_ROLE_CODE);
    }

    public String getRoleName() {
        return get("role-name", RbacConstants.DEFAULT_COLUMN_ROLE_NAME);
    }

    public String getApiId() {
        return get("api-id", RbacConstants.DEFAULT_COLUMN_API_ID);
    }

    public String getRoleId() {
        return get("role-id", RbacConstants.DEFAULT_COLUMN_ROLE_ID);
    }

    public String getUserId() {
        return get("user-id", RbacConstants.DEFAULT_COLUMN_USER_ID);
    }

    public String getCreatedAt() {
        return get("created-at", RbacConstants.DEFAULT_COLUMN_CREATED_AT);
    }

    private String get(String logicalName, String defaultCol) {
        if (mapping != null && mapping.containsKey(logicalName)) {
            return mapping.get(logicalName);
        }
        return defaultCol;
    }
}
