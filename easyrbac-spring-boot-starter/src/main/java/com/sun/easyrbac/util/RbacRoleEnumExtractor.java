package com.sun.easyrbac.util;

import com.sun.easyrbac.annotation.RbacRole;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从声明了 {@link RbacRole} 的枚举类中提取角色编码。
 * <p>
 * 为避免每次请求都进行反射扫描，按枚举类做一次性缓存。
 */
public final class RbacRoleEnumExtractor {

    private static final Map<Class<? extends Enum<?>>, List<String>> CACHE = new ConcurrentHashMap<>();

    private RbacRoleEnumExtractor() {
    }

    /**
     * 从若干枚举类中提取所有通过 {@link RbacRole} 声明的角色编码。
     *
     * @param enums 枚举类数组
     * @return 角色编码集合（可能为空，但不会为 null）
     */
    public static List<String> extractCodes(Class<? extends Enum<?>>[] enums) {
        if (enums == null || enums.length == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Class<? extends Enum<?>> enumClass : enums) {
            if (enumClass == null) {
                continue;
            }
            result.addAll(getOrLoad(enumClass));
        }
        return result;
    }

    private static List<String> getOrLoad(Class<? extends Enum<?>> enumClass) {
        return CACHE.computeIfAbsent(enumClass, RbacRoleEnumExtractor::loadFromEnum);
    }

    private static List<String> loadFromEnum(Class<? extends Enum<?>> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        for (Object constant : constants) {
            if (!(constant instanceof Enum<?> e)) {
                continue;
            }
            String name = e.name();
            try {
                Field field = enumClass.getField(name);
                RbacRole anno = field.getAnnotation(RbacRole.class);
                if (anno == null) {
                    continue;
                }
                String code = anno.value();
                if (code == null) {
                    continue;
                }
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    codes.add(trimmed);
                }
            } catch (NoSuchFieldException ex) {
                // 正常情况下不会发生，忽略即可
            }
        }
        if (codes.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(codes);
    }
}

