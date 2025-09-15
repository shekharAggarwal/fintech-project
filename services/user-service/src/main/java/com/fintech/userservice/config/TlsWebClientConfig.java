package com.fintech.userservice.config;

import io.micrometer.observation.ObservationRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

/**
 * Configuration for REST client communication
 */
@Configuration
public class TlsWebClientConfig {




    @Value("${tls.client.key-store}")
    private Resource keyStore;

    @Value("${tls.client.key-store-password}")
    private String keyStorePassword;

    @Value("${tls.client.trust-store}")
    private Resource trustStore;

    @Value("${tls.client.trust-store-password}")
    private String trustStorePassword;

    private final ObservationRegistry observationRegistry;

    public TlsWebClientConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Bean
    public WebClient authzWebClient() throws Exception {
        // load client key material (PKCS12)
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(keyStore.getInputStream(), keyStorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());

        // load truststore (JKS)
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(trustStore.getInputStream(), trustStorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(kmf)
                .trustManager(tmf)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .responseTimeout(java.time.Duration.ofMillis(5000)) // 5 second response timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000); // 3 second connect timeout

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB buffer
                .observationRegistry(observationRegistry) // Enable tracing/observability
                .build();
    }
}
