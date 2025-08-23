package com.fintech.gatewayservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
public class JwtConfig {
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

    public boolean validateJwt(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            // Optionally, add more claim checks here
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
