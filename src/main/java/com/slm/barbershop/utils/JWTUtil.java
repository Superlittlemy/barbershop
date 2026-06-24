package com.slm.barbershop.utils;

import com.slm.barbershop.exception.BizException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {

    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;

    public static final String MEMBER_TOKEN_SUBJECT_PREFIX = "member:";

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final String issuer = "barbershop";
    private final String subject = "barbershop-user";

    public JWTUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateJwtToken(Long id, String username) {
        return generateJwtToken(id, username, subject);
    }

    public String generateJwtToken(Long id, String username, String subject) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claim("id", id)
                .claim("username", username)
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
                .issuedAt(new Date())
                .subject(subject)
                .issuer(issuer)
                .signWith(key, ALGORITHM)
                .compact();
    }

    public Claims getClaimsFromJwt(String jwt) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            throw new BizException(HttpStatus.BAD_REQUEST, "无效token");
        }
    }

    public Long getUserIdFromJwt(String jwt) {
        Claims claims = getClaimsFromJwt(jwt);
        return claims.get("id", Long.class);
    }

    public String getUsernameFromJwt(String jwt) {
        Claims claims = getClaimsFromJwt(jwt);
        return claims.get("username", String.class);
    }

}
