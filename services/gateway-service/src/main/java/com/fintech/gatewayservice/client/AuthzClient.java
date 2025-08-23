package com.fintech.gatewayservice.client;

import com.fintech.gatewayservice.dto.request.AuthzIntrospectRequest;
import com.fintech.gatewayservice.dto.response.AuthzIntrospectResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthzClient {

    private final WebClient webClient;

    public AuthzClient(WebClient authzWebClient) {
        this.webClient = authzWebClient;
    }

    public Mono<AuthzIntrospectResponse> introspect(AuthzIntrospectRequest req, int timeoutMs) {
        return webClient.post()
                .uri("/api/authz/introspect")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(AuthzIntrospectResponse.class)
                .timeout(java.time.Duration.ofMillis(timeoutMs));
    }
}