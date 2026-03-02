package com.sun.easyrbac.exception;

import com.sun.easyrbac.constant.RbacConstants;

/**
 * RBAC 框架统一异常。
 * 携带错误码便于文档与排查，升级时保持兼容。
 *
 * @author SUNRUI
 * @see RbacConstants#ERR_CONFIG_REQUIRED
 * @see RbacConstants#ERR_FIELD_MAPPING
 * @see RbacConstants#ERR_DB_OPERATION
 * @see RbacConstants#ERR_ACCESS_DENIED
 * @see RbacConstants#ERR_TOKEN_INVALID
 */
public class RbacException extends RuntimeException {

    private final String errorCode;
    private final String message;

    public RbacException(String errorCode, String message) {
        super("[" + errorCode + "] " + message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public RbacException(String errorCode, String message, Throwable cause) {
        super("[" + errorCode + "] " + message, cause);
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
