package com.sun.easyrbac.core.repository;

import com.sun.easyrbac.core.domain.RbacApi;

import java.util.List;
import java.util.Optional;

/**
 * 接口/权限表仓储接口。
 *
 * @author SUNRUI
 */
public interface RbacApiRepository {

    Optional<RbacApi> findByPath(String path);

    List<RbacApi> findAll();

    void save(RbacApi api);

    void saveAll(List<RbacApi> apis);
}
