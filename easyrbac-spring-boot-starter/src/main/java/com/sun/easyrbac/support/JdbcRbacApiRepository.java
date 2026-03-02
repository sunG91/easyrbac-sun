package com.sun.easyrbac.support;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.domain.RbacApi;
import com.sun.easyrbac.core.repository.RbacApiRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * 接口/权限表 JDBC 实现。
 *
 * @author SUNRUI
 */
public class JdbcRbacApiRepository implements RbacApiRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String colId;
    private final String colPath;
    private final String colMethod;
    private final String colControllerName;

    public JdbcRbacApiRepository(JdbcTemplate jdbcTemplate, RbacProperties properties, RbacColumnResolver columns) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = properties.getAuto().getApiTable();
        this.colId = columns.getId();
        this.colPath = columns.getPath();
        this.colMethod = "method";
        this.colControllerName = "controller_name";
    }

    private RowMapper<RbacApi> rowMapper() {
        return (rs, i) -> {
            RbacApi a = new RbacApi();
            a.setId(rs.getLong(colId));
            a.setPath(rs.getString(colPath));
            a.setMethod(rs.getString(colMethod));
            a.setControllerName(rs.getString(colControllerName));
            return a;
        };
    }

    @Override
    public Optional<RbacApi> findByPath(String path) {
        String sql = "SELECT " + colId + "," + colPath + "," + colMethod + "," + colControllerName +
                " FROM " + tableName + " WHERE " + colPath + " = ?";
        List<RbacApi> list = jdbcTemplate.query(sql, rowMapper(), path);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<RbacApi> findAll() {
        String sql = "SELECT " + colId + "," + colPath + "," + colMethod + "," + colControllerName + " FROM " + tableName;
        return jdbcTemplate.query(sql, rowMapper());
    }

    @Override
    public void save(RbacApi api) {
        if (api.getId() != null && api.getId() > 0) {
            String sql = "UPDATE " + tableName + " SET " + colMethod + " = ?, " + colControllerName + " = ? WHERE " + colId + " = ?";
            jdbcTemplate.update(sql, api.getMethod(), api.getControllerName(), api.getId());
        } else {
            String sql = "INSERT INTO " + tableName + " (" + colPath + "," + colMethod + "," + colControllerName + ") VALUES (?,?,?)";
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, api.getPath());
                ps.setString(2, api.getMethod() != null ? api.getMethod() : "");
                ps.setString(3, api.getControllerName() != null ? api.getControllerName() : "");
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                api.setId(key.longValue());
            }
        }
    }

    @Override
    public void saveAll(List<RbacApi> apis) {
        for (RbacApi api : apis) {
            save(api);
        }
    }
}
