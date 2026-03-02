package com.sun.easyrbac.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段映射构建器，供 {@link RbacFieldMappingCustomizer} 使用。
 *
 * @author SUNRUI
 */
public class FieldMappingBuilder {

    private final Map<String, String> mapping = new LinkedHashMap<>();

    /** 逻辑名 id → 数据库列名 */
    public FieldMappingBuilder mapIdTo(String columnName) {
        mapping.put("id", columnName);
        return this;
    }

    /** 逻辑名 path → 数据库列名 */
    public FieldMappingBuilder mapPathTo(String columnName) {
        mapping.put("path", columnName);
        return this;
    }

    /** 添加逻辑名 → 列名（如 role_code → role_code） */
    public FieldMappingBuilder addMapping(String logicalName, String columnName) {
        mapping.put(logicalName, columnName);
        return this;
    }

    /** 移除某逻辑名映射 */
    public FieldMappingBuilder removeMapping(String logicalName) {
        mapping.remove(logicalName);
        return this;
    }

    public Map<String, String> build() {
        return new LinkedHashMap<>(mapping);
    }
}
