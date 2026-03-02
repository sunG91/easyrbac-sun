package com.sun.easyrbac.core.repository;

import com.sun.easyrbac.core.domain.RbacRole;

import java.util.List;
import java.util.Optional;

/**
 * 角色表仓储接口。
 * 由 starter 层提供 JDBC 等实现，便于 2.0/3.0 替换为其他存储。
 *
 * @author SUNRUI
 */
public interface RbacRoleRepository {

    /**
     * 按角色编码查询
     */
    Optional<RbacRole> findByRoleCode(String roleCode);

    /**
     * 查询所有角色
     */
    List<RbacRole> findAll();

    /**
     * 保存或更新角色（存在则更新名称等）
     */
    void save(RbacRole role);

    /**
     * 批量保存或更新
     */
    void saveAll(List<RbacRole> roles);
}
