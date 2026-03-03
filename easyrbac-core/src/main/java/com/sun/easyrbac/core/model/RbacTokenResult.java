package com.sun.easyrbac.core.model;

/**
 * 登录后签发 Token 的返回结果，便于业务直接返回给前端。
 * 业务只需在登录校验通过后调用 {@code RbacTokenIssuerService.issueToken(userId)}，将本结果放入响应即可。
 *
 * @author SUNRUI
 */
public class RbacTokenResult {

    private String token;
    private String type;
    private long expireSeconds;
    private String userId;

    public RbacTokenResult() {}

    public RbacTokenResult(String token, String type, long expireSeconds, String userId) {
        this.token = token;
        this.type = type;
        this.expireSeconds = expireSeconds;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
