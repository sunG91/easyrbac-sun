package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.repository.RbacUserRoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 用户-角色关联表 JDBC 实现。
 * 用户-角色由业务维护，不参与启动时自动同步。
 *
 * @author SUNRUI
 */
public class JdbcRbacUserRoleRepository implements RbacUserRoleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String colUserId;
    private final String colRoleId;

    public JdbcRbacUserRoleRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = properties.getAuto().getUserRoleTable();
        this.colUserId = columns.getUserId();
        this.colRoleId = columns.getRoleId();
    }

    @Override
    public void addRole(String userId, Long roleId) {
        String sql = "INSERT INTO " + tableName + " (" + colUserId + "," + colRoleId + ") VALUES (?,?)";
        try {
            jdbcTemplate.update(sql, userId, roleId);
        } catch (Exception e) {
            // 唯一约束冲突时忽略
        }
    }

    @Override
    public void removeRole(String userId, Long roleId) {
        String sql = "DELETE FROM " + tableName + " WHERE " + colUserId + " = ? AND " + colRoleId + " = ?";
        jdbcTemplate.update(sql, userId, roleId);
    }

    @Override
    public List<Long> findRoleIdsByUserId(String userId) {
        String sql = "SELECT " + colRoleId + " FROM " + tableName + " WHERE " + colUserId + " = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }

    @Override
    public boolean hasRole(String userId, Long roleId) {
        String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE " + colUserId + " = ? AND " + colRoleId + " = ?";
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class, userId, roleId);
        return c != null && c > 0;
    }
}
