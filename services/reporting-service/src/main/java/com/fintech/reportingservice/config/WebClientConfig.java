package com.fintech.reportingservice.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * WebClient configuration with mTLS for external service calls
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${fintech.ssl.keystore.path:classpath:certs/fintech.p12}")
    private String keystorePath;

    @Value("${fintech.ssl.keystore.password:changeit}")
    private String keystorePassword;

    @Value("${fintech.ssl.truststore.path:classpath:certs/fintech-truststore.jks}")
    private String truststorePath;

    @Value("${fintech.ssl.truststore.password:changeit}")
    private String truststorePassword;

    @Bean
    public WebClient webClient() {
        try {
            // Load keystore for client certificate
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keystorePath.replace("classpath:", "certs/")), 
                keystorePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

            // Load truststore for server certificate validation
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(truststorePath.replace("classpath:", "certs/")), 
                truststorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Create SSL context with mTLS
            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(keyManagerFactory)
                    .trustManager(trustManagerFactory)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            log.error("Failed to configure mTLS WebClient, falling back to default", e);
            return WebClient.builder().build();
        }
    }

    @Bean
    public WebClient defaultWebClient() {
        return WebClient.builder().build();
    }
}