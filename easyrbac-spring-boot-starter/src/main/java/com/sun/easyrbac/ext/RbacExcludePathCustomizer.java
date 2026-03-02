package com.sun.easyrbac.ext;

import java.util.List;

/**
 * 扩展点：动态返回不参与权限校验的路径列表（与 YAML exclude-paths 取并集）。
 *
 * @author SUNRUI
 */
public interface RbacExcludePathCustomizer {

    /**
     * 返回放开的路径，支持 Ant 风格（如 /public/**）。
     * 实现内尽量返回内存或缓存结果，避免每次请求执行重量级操作。
     */
    List<String> getExcludePaths();
}
