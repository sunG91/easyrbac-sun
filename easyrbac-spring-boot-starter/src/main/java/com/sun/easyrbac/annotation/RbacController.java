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

    /**
     * 默认需要的角色编码/权限 ID（与 id 等价）。
     * 支持单个或多个值：@RbacController("10000") / @RbacController({"10000","10001"})
     */
    String[] value() default {};

    /**
     * 显式指定权限 ID。
     * 支持单个或多个值：id = "P_USER_LIST" / id = {"P_USER_LIST","P_USER_EXPORT"}
     */
    String[] id() default {};

    /**
     * 权限地址（仅 annotation-mode=path 时生效，须显式指定）。
     * 支持单个或多个值：path = "/user/list" / path = {"/user/list","/user/export"}
     */
    String[] path() default {};

    /**
     * 参与本 Controller 下接口默认权限判断的“角色枚举类”。
     * <p>
     * 每个枚举常量可使用 {@link RbacRole} 标注，其 {@link RbacRole#value()} 会被解析为角色编码，
     * 并与 {@link #value()} / {@link #id()} / {@link #path()} 一并作为类级默认角色参与权限判断。
     */
    Class<? extends Enum<?>>[] roleEnums() default {};
}
