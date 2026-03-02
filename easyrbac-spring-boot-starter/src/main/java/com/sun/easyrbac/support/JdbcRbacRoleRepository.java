package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.domain.RbacRole;
import com.sun.easyrbac.core.repository.RbacRoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * 角色表 JDBC 实现。
 *
 * @author SUNRUI
 */
public class JdbcRbacRoleRepository implements RbacRoleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String colId;
    private final String colRoleCode;
    private final String colRoleName;

    public JdbcRbacRoleRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = properties.getAuto().getRoleTable();
        this.colId = columns.getId();
        this.colRoleCode = columns.getRoleCode();
        this.colRoleName = columns.getRoleName();
    }

    private RowMapper<RbacRole> rowMapper() {
        return (rs, i) -> {
            RbacRole r = new RbacRole();
            r.setId(rs.getLong(colId));
            r.setRoleCode(rs.getString(colRoleCode));
            r.setRoleName(rs.getString(colRoleName));
            return r;
        };
    }

    @Override
    public Optional<RbacRole> findByRoleCode(String roleCode) {
        String sql = "SELECT " + colId + "," + colRoleCode + "," + colRoleName +
                " FROM " + tableName + " WHERE " + colRoleCode + " = ?";
        List<RbacRole> list = jdbcTemplate.query(sql, rowMapper(), roleCode);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<RbacRole> findAll() {
        String sql = "SELECT " + colId + "," + colRoleCode + "," + colRoleName + " FROM " + tableName;
        return jdbcTemplate.query(sql, rowMapper());
    }

    @Override
    public void save(RbacRole role) {
        if (role.getId() != null && role.getId() > 0) {
            String sql = "UPDATE " + tableName + " SET " + colRoleName + " = ? WHERE " + colId + " = ?";
            jdbcTemplate.update(sql, role.getRoleName(), role.getId());
        } else {
            String sql = "INSERT INTO " + tableName + " (" + colRoleCode + "," + colRoleName + ") VALUES (?,?)";
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, role.getRoleCode());
                ps.setString(2, role.getRoleName());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                role.setId(key.longValue());
            }
        }
    }

    @Override
    public void saveAll(List<RbacRole> roles) {
        for (RbacRole role : roles) {
            save(role);
        }
    }
}
