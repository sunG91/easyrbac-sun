package com.sun.easyrbac.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在枚举常量上，表示该枚举对应的角色编码与角色名。
 * 配置 rbac.auto.role-enum-class 后，可从枚举同步角色表，此时可不配置 YAML 的 role-mapping。
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

    /**
     * 角色名称（展示用），如 "超级管理员"。为空时使用枚举常量名。
     */
    String name() default "";
}
