package com.sun.easyrbac.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在枚举常量上，表示该枚举对应的角色编码（与 role-mapping 的 key 一致）。
 * 需在 YAML 中配置 rbac.auto.role-enum-class 为枚举全限定名。
 *
 * @author SUNRUI
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RbacRole {

    /**
     * 角色编码，如 "10000"
     */
    String value();
}
