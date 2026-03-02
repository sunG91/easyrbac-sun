package com.sun.easyrbac.core.repository;

import java.util.List;

/**
 * 角色-接口关联表仓储接口。
 *
 * @author SUNRUI
 */
public interface RbacRoleApiRepository {

    /**
     * 绑定角色与接口
     */
    void bindRoleApi(Long roleId, Long apiId);

    /**
     * 批量绑定：角色下的所有接口
     */
    void bindRoleApis(Long roleId, List<Long> apiIds);

    /**
     * 查询某角色已绑定的接口 ID 列表
     */
    List<Long> findApiIdsByRoleId(Long roleId);

    /**
     * 查询某接口已绑定的角色 ID 列表
     */
    List<Long> findRoleIdsByApiId(Long apiId);
}
