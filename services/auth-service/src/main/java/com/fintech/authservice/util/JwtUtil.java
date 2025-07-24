package com.fintech.authservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    
    @Value("${jwt.keystore-path}")
    private String keystorePath;
    
    @Value("${jwt.keystore-password}")
    private String keystorePassword;
    
    @Value("${jwt.key-alias}")
    private String keyAlias;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    private PrivateKey privateKey;
    private PublicKey publicKey;
    
    public String generateAccessToken(String email, String sessionId) {
        return generateToken(email, sessionId, accessTokenExpiration);
    }
    
    public String generateRefreshToken(String email, String sessionId) {
        return generateToken(email, sessionId, refreshTokenExpiration);
    }
    
    private String generateToken(String email, String sessionId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .claim("sessionId", sessionId)
                .signWith(getPrivateKey())
                .compact();
    }
    
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }
    
    public String getSessionIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("sessionId", String.class);
    }
    
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("role", String.class);
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
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(getClass().getResourceAsStream(keystorePath), 
                             keystorePassword.toCharArray());
                privateKey = (PrivateKey) keyStore.getKey(keyAlias, 
                                                         keystorePassword.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load private key", e);
            }
        }
        return privateKey;
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
