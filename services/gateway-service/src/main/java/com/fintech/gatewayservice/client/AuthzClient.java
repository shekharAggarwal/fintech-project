package com.fintech.gatewayservice.client;

import com.fintech.gatewayservice.dto.request.AuthzIntrospectRequest;
import com.fintech.gatewayservice.dto.response.AuthzIntrospectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class AuthzClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthzClient.class);
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
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .filter(throwable -> throwable instanceof WebClientRequestException ||
                                throwable instanceof TimeoutException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.warn("Retry exhausted for authz introspect after {} attempts",
                                    retrySignal.totalRetries());
                            return retrySignal.failure();
                        }))
                .doOnError(ex -> {
                    if (ex instanceof TimeoutException) {
                        logger.error("Authz service timeout after {}ms for path={} method={}",
                                timeoutMs, req.path, req.method);
                    } else if (ex instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) ex;
                        logger.error("Authz service error {} for path={} method={}: {}",
                                wcre.getStatusCode(), req.path, req.method, wcre.getResponseBodyAsString());
                    } else {
                        logger.error("Authz service unexpected error for path={} method={}",
                                req.path, req.method, ex);
                    }
                })
                .onErrorReturn(createDeniedResponse("AUTHZ_SERVICE_ERROR"));
    }

    private AuthzIntrospectResponse createDeniedResponse(String reason) {
        AuthzIntrospectResponse response = new AuthzIntrospectResponse();
        response.allowed = false;
        response.reason = reason;
        return response;
    }
}