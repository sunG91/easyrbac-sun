package com.sun.easyrbac.token;

import com.sun.easyrbac.config.RbacProperties;
import com.sun.easyrbac.core.token.RbacTokenValidator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 标准 JWT 校验：解析 payload 取 sub 作为 userId，校验 exp 过期。
 * 未引入 jjwt 时做最小实现（Base64 解码 + exp 校验）；生产建议引入 jjwt 并做签名校验。
 *
 * @author SUNRUI
 */
public class JwtTokenValidator implements RbacTokenValidator {

    private final RbacProperties.Jwt jwtConfig;

    public JwtTokenValidator(RbacProperties properties) {
        this.jwtConfig = properties.getCheck().getJwt();
    }

    @Override
    public String validate(String token) {
        if (token == null || token.isEmpty()) return null;
        int dot1 = token.indexOf('.');
        int dot2 = token.indexOf('.', dot1 + 1);
        if (dot1 <= 0 || dot2 <= dot1) return null;
        try {
            String payloadB64 = token.substring(dot1 + 1, dot2);
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64.replace('-', '+').replace('_', '/')), StandardCharsets.UTF_8);
            long exp = extractLong(payload, "exp");
            if (exp > 0 && System.currentTimeMillis() / 1000 > exp) return null;
            String sub = extractString(payload, "sub");
            return sub;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generate(Object userId) {
        return null;
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int i = json.indexOf(search);
        if (i < 0) return 0;
        i += search.length();
        int j = i;
        while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j) == '-')) j++;
        if (j == i) return 0;
        try {
            return Long.parseLong(json.substring(i, j));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        i += search.length();
        int j = json.indexOf('"', i);
        if (j < 0) return null;
        return json.substring(i, j);
    }
}
