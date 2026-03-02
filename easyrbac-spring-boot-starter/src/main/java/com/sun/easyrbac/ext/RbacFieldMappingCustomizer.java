package com.sun.easyrbac.ext;

/**
 * 扩展点：在代码中自定义表字段映射（与 YAML field-mapping 可叠加，以代码为准或按实现合并）。
 * 实现此接口并注册为 Spring Bean，框架在解析列名时会调用。
 *
 * @author SUNRUI
 */
public interface RbacFieldMappingCustomizer {

    /**
     * 自定义 id、path 等逻辑名到数据库列名的映射。
     * 必配：mapIdTo、mapPathTo 至少各配置一次。
     */
    void customize(FieldMappingBuilder builder);
}
