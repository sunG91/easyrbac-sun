package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.exception.RbacException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 根据配置创建 RBAC 四张表。
 * 使用 field-mapping 解析列名，未配置则用默认列名。
 *
 * @author SUNRUI
 */
public class RbacSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(RbacSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final RbacProperties properties;
    private final RbacColumnResolver columns;

    public RbacSchemaInitializer(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.columns = columns;
    }

    /**
     * 若开启 auto-create-table 则建表（不存在时创建）。
     */
    public void createTablesIfNeeded() {
        if (!properties.isEnabled() || !properties.getAuto().isAutoCreateTable()) {
            return;
        }
        String idCol = columns.getId();
        String pathCol = columns.getPath();
        String roleCodeCol = columns.getRoleCode();
        String roleNameCol = columns.getRoleName();
        String roleIdCol = columns.getRoleId();
        String apiIdCol = columns.getApiId();
        String userIdCol = columns.getUserId();

        String roleTable = properties.getAuto().getRoleTable();
        String apiTable = properties.getAuto().getApiTable();
        String roleApiTable = properties.getAuto().getRoleApiTable();
        String userRoleTable = properties.getAuto().getUserRoleTable();

        try {
            createRoleTable(roleTable, idCol, roleCodeCol, roleNameCol);
            createApiTable(apiTable, idCol, pathCol);
            createRoleApiTable(roleApiTable, roleIdCol, apiIdCol);
            createUserRoleTable(userRoleTable, userIdCol, roleIdCol);
            log.info("[EasyRBAC] 表结构检查/创建完成: {}, {}, {}, {}", roleTable, apiTable, roleApiTable, userRoleTable);
        } catch (Exception e) {
            throw new RbacException(RbacConstants.ERR_DB_OPERATION, "建表失败: " + e.getMessage(), e);
        }
    }

    private void createRoleTable(String table, String id, String roleCode, String roleName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                id + " BIGINT AUTO_INCREMENT PRIMARY KEY," +
                roleCode + " VARCHAR(64) NOT NULL," +
                roleName + " VARCHAR(128), UNIQUE KEY uk_" + table + "_code (" + roleCode + "))";
        jdbcTemplate.execute(sql);
    }

    private void createApiTable(String table, String id, String path) {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                id + " BIGINT AUTO_INCREMENT PRIMARY KEY," +
                path + " VARCHAR(256) NOT NULL," +
                "method VARCHAR(32)," +
                "controller_name VARCHAR(256), UNIQUE KEY uk_" + table + "_path (" + path + "))";
        jdbcTemplate.execute(sql);
    }

    private void createRoleApiTable(String table, String roleId, String apiId) {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                roleId + " BIGINT NOT NULL," +
                apiId + " BIGINT NOT NULL," +
                "PRIMARY KEY (" + roleId + "," + apiId + "))";
        jdbcTemplate.execute(sql);
    }

    private void createUserRoleTable(String table, String userId, String roleId) {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                userId + " VARCHAR(64) NOT NULL," +
                roleId + " BIGINT NOT NULL," +
                "PRIMARY KEY (" + userId + "," + roleId + "))";
        jdbcTemplate.execute(sql);
    }
}
