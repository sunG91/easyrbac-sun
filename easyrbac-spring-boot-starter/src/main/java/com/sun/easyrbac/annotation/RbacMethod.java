package com.sun.easyrbac.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法上，表示该接口需要的角色/权限。
 * 优先于类上的 {@link RbacController}。
 * 直接写 value 或 id 表示权限 ID；使用 path 须显式写 path = "..."。
 *
 * @author SUNRUI
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RbacMethod {

    /**
     * 需要的角色编码/权限 ID（与 id 等价）。
     * 支持单个或多个值：@RbacMethod("10000") / @RbacMethod({"10000","10001"})
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
     * 参与本接口权限判断的“角色枚举类”。
     * <p>
     * 每个枚举常量可使用 {@link RbacRole} 标注，其 {@link RbacRole#value()} 会被解析为角色编码，
     * 并与 {@link #value()} / {@link #id()} / {@link #path()} 一并参与当前接口的权限判断。
     * <p>
     * 示例：
     * <pre>{@code
     * public enum AppRole {
     *     @RbacRole(value = "10000", name = "管理员")
     *     OPS,
     *     @RbacRole(value = "10001", name = "运营")
     *     OPS2
     * }
     *
     * @RbacMethod(roleEnums = {AppRole.class})
     * public Result list() { ... }
     * }</pre>
     */
    Class<? extends Enum<?>>[] roleEnums() default {};
}
