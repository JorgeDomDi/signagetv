package com.signagetv.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-hours:24}")
    private long expirationHours;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes;
        // Permite secret en Base64 o texto plano (mínimo 32 bytes)
        try {
            bytes = Decoders.BASE64.decode(secret);
            if (bytes.length < 32) bytes = secret.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        log.info("JwtService inicializado (HS256, {}h expiración)", expirationHours);
    }

    public String generate(Long localId, String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationHours * 3600_000L);
        return Jwts.builder()
                .subject(String.valueOf(localId))
                .claim("username", username)
                .claim("localId", localId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
