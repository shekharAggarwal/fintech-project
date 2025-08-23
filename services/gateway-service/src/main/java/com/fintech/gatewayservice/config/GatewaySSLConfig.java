package com.fintech.gatewayservice.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class GatewaySSLConfig {

    @Value("${tls.client.key-store}")
    private Resource keyStore;

    @Value("${tls.client.key-store-password}")
    private String keyStorePassword;

    @Value("${tls.client.trust-store}")
    private Resource trustStore;

    @Value("${tls.client.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public HttpClientCustomizer httpClientCustomizer() throws Exception {
        // Load client key material (PKCS12) for mTLS
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(keyStore.getInputStream(), keyStorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());

        // Load truststore (JKS) for certificate validation
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(trustStore.getInputStream(), trustStorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(kmf)  // Client certificate for mTLS
                .trustManager(tmf)  // Server certificate validation
                .build();

        return httpClient -> httpClient.secure(spec -> 
            spec.sslContext(sslContext)
                .handshakeTimeout(java.time.Duration.ofSeconds(10))
                .closeNotifyFlushTimeout(java.time.Duration.ofSeconds(3))
        );
    }
}
