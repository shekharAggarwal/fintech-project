package com.fintech.authorizationservice.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.PublicKey;

@Component
public class JwtUtil {
    
    @Value("${JWT_KEYSTORE_PATH:/jwt-keystore.p12}")
    private String keystorePath;
    
    @Value("${JWT_KEYSTORE_PASSWORD:fintech123}")
    private String keystorePassword;
    
    @Value("${JWT_KEY_ALIAS:fintech-jwt}")
    private String keyAlias;
    
    private PublicKey publicKey;
    
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    public String getSessionIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("sessionId", String.class) : null;
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
    
    private PublicKey getPublicKey() {
        if (publicKey == null) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(getClass().getResourceAsStream(keystorePath), 
                             keystorePassword.toCharArray());
                publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load public key", e);
            }
        }
        return publicKey;
    }
}
