package com.sun.easyrbac.core.token;

/**
 * Token 校验接口：在业务代码中校验某段 token 是否合法。
 * 由 starter 层提供 internal/jwt 实现。
 *
 * @author SUNRUI
 */
public interface RbacTokenValidator {

    /**
     * 校验 token 是否有效（签名、未过期等），若有效返回解析出的用户标识（如 userId），否则返回 null。
     */
    String validate(String token);

    /**
     * 签发 token，将用户标识写入，返回 token 字符串。
     */
    String generate(String userId);
}
