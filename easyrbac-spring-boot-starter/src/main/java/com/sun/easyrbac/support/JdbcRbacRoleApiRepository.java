package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.repository.RbacRoleApiRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 角色-接口关联表 JDBC 实现。
 *
 * @author SUNRUI
 */
public class JdbcRbacRoleApiRepository implements RbacRoleApiRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String colRoleId;
    private final String colApiId;

    public JdbcRbacRoleApiRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = properties.getAuto().getRoleApiTable();
        this.colRoleId = columns.getRoleId();
        this.colApiId = columns.getApiId();
    }

    @Override
    public void bindRoleApi(Long roleId, Long apiId) {
        String sql = "INSERT INTO " + tableName + " (" + colRoleId + "," + colApiId + ") VALUES (?,?)";
        try {
            jdbcTemplate.update(sql, roleId, apiId);
        } catch (Exception e) {
            // 唯一约束冲突时忽略（已存在绑定）
        }
    }

    @Override
    public void bindRoleApis(Long roleId, List<Long> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return;
        }
        for (Long apiId : apiIds) {
            bindRoleApi(roleId, apiId);
        }
    }

    @Override
    public List<Long> findApiIdsByRoleId(Long roleId) {
        String sql = "SELECT " + colApiId + " FROM " + tableName + " WHERE " + colRoleId + " = ?";
        return jdbcTemplate.queryForList(sql, Long.class, roleId);
    }

    @Override
    public List<Long> findRoleIdsByApiId(Long apiId) {
        String sql = "SELECT " + colRoleId + " FROM " + tableName + " WHERE " + colApiId + " = ?";
        return jdbcTemplate.queryForList(sql, Long.class, apiId);
    }
}
