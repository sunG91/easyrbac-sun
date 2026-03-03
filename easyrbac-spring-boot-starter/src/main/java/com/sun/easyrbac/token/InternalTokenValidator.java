package com.sun.easyrbac.token;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.constant.RbacConstants;
import com.sun.easyrbac.core.token.RbacTokenValidator;
import com.sun.easyrbac.exception.RbacException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 框架自研 Token：payload 为 userId:expireAt，HMAC-SHA256 签名。
 * 无需 JWT 依赖即可完成签发与校验。
 *
 * @author SUNRUI
 */
public class InternalTokenValidator implements RbacTokenValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final String secret;
    private final long expireSeconds;
    private final String prefix;

    public InternalTokenValidator(RbacProperties properties) {
        RbacProperties.Internal internal = properties.getCheck().getInternal();
        String s = internal.getSecret();
        if (s == null || s.isEmpty()) {
            s = "easyrbac-internal-default-secret-change-in-production-" + System.currentTimeMillis();
        }
        this.secret = s;
        this.expireSeconds = internal.getExpireSeconds() > 0 ? internal.getExpireSeconds() : 7200;
        this.prefix = internal.getPrefix() != null ? internal.getPrefix() : "Bearer";
    }

    @Override
    public String generate(Object userId) {
        if (userId == null) return null;
        String idStr = String.valueOf(userId);
        long expireAt = System.currentTimeMillis() + expireSeconds * 1000L;
        String payload = idStr + ":" + expireAt;
        String sign = hmac(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String validate(String token) {
        if (token == null || token.isEmpty()) return null;
        // 兼容传入带前缀的值，如 "Bearer xxx"：自动剥离前缀，避免调用方做多余处理
        if (prefix != null && !prefix.isEmpty() && token.startsWith(prefix + " ")) {
            token = token.substring(prefix.length()).trim();
        }
        int dot = token.indexOf('.');
        if (dot <= 0) return null;
        try {
            String payloadB64 = token.substring(0, dot);
            String signB64 = token.substring(dot + 1);
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String expectedSign = hmac(payload);
            String actualSign = new String(Base64.getUrlDecoder().decode(signB64), StandardCharsets.UTF_8);
            if (!expectedSign.equals(actualSign)) return null;
            int colon = payload.indexOf(':');
            if (colon <= 0) return null;
            String userId = payload.substring(0, colon);
            long expireAt = Long.parseLong(payload.substring(colon + 1));
            if (System.currentTimeMillis() > expireAt) return null;
            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RbacException(RbacConstants.ERR_TOKEN_INVALID, "Internal token HMAC init failed", e);
        }
    }
}
