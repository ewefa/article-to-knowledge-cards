package com.campus.trade.security;

import com.campus.trade.config.CampusProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具：签发、解析令牌
 */
@Slf4j
@Component
public class JwtUtil {

    /** HS256 要求密钥不少于 32 字节 */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    private final long expireSeconds;

    public JwtUtil(CampusProperties properties) {
        this.key = resolveKey(properties.getJwt().getSecret());
        this.expireSeconds = properties.getJwt().getExpireHours() * 3600;
    }

    /**
     * 密钥优先取配置（环境变量 CAMPUS_JWT_SECRET）；未配置或过短时随机生成，
     * 这样仓库里不需要保存任何签名密钥，代价是重启后旧令牌全部失效。
     */
    private static SecretKey resolveKey(String secret) {
        if (secret != null && !secret.isBlank()) {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length >= MIN_SECRET_BYTES) {
                return Keys.hmacShaKeyFor(bytes);
            }
            log.warn("campus.jwt.secret 长度不足 {} 字节，已改用本次启动随机生成的密钥", MIN_SECRET_BYTES);
        } else {
            log.warn("未配置 campus.jwt.secret（环境变量 CAMPUS_JWT_SECRET），本次启动使用随机密钥：重启后已签发的令牌将全部失效");
        }
        byte[] random = new byte[MIN_SECRET_BYTES];
        new SecureRandom().nextBytes(random);
        return Keys.hmacShaKeyFor(random);
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /** 签发令牌，subject 为用户 ID，附带账号、姓名与角色 */
    public String createToken(Long userId, String account, String name, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("account", account)
                .claim("name", name)
                .claim("role", role)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /** 解析令牌，非法或过期返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** 从令牌中取用户 ID，非法令牌返回 null */
    public Long getUserId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
