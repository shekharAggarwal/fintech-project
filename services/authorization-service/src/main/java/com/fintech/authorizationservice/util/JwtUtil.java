package com.fintech.authorizationservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
public class JwtUtil {

    @Value("${jwt.public-cert-path}")
    private String publicCertPath;

    private PublicKey publicKey;

    @PostConstruct
    public void loadPublicKey() {
        try {
            InputStream in = ResourceUtils.getURL(publicCertPath).openStream();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
            this.publicKey = cert.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key for JWT validation", e);
        }
    }


    public String getSessionIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("sessionId", String.class) : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

}
