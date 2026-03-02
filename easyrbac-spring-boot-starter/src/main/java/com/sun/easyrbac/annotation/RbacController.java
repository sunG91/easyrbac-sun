package com.sun.easyrbac.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 类上，表示该类下接口需要的默认角色/权限。
 * 可与 {@link RbacMethod} 配合：方法级注解优先于类级。
 * 直接写 value 或 id 表示权限 ID；使用 path 须显式写 path = "..."。
 *
 * @author SUNRUI
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RbacController {

    /** 默认需要的角色编码/权限 ID（与 id 等价） */
    String value() default "";

    /** 显式指定权限 ID */
    String id() default "";

    /** 权限地址（仅 annotation-mode=path 时生效，须显式指定） */
    String path() default "";
}
