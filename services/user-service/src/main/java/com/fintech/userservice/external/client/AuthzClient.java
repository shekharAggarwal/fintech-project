package com.fintech.userservice.external.client;

import com.fintech.userservice.external.model.request.UpdateRoleRequest;
import com.fintech.userservice.external.model.response.UpdateRoleResponse;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final Tracer tracer;

    @Value("${service.authz.base-url}")
    private String authorizationServiceUrl;
    @Value("${gateway.authz.timeout-ms:1200}")
    private int timeoutMs;

    public AuthzClient(WebClient authzWebClient, Tracer tracer) {
        this.webClient = authzWebClient;
        this.tracer = tracer;
    }

    public Mono<UpdateRoleResponse> updateUserRole(UpdateRoleRequest req) {

        return Mono.fromCallable(() -> req)
                .flatMap(request ->
                        webClient.post()
                                .uri(authorizationServiceUrl + "/api/authz/internal/user-role")
                                .bodyValue(req)
                                .retrieve()
                                .bodyToMono(UpdateRoleResponse.class)
                )
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
                        logger.error("Authz service timeout after {}ms for new role: {} updated by: {} for userId: {} ",
                                timeoutMs, req.newRole(), req.updatedBy(), req.userId());
                    } else if (ex instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) ex;
                        logger.error("Authz service error {} for  new role: {} updated by: {} for userId: {}: {}",
                                wcre.getStatusCode(), req.newRole(), req.updatedBy(), req.userId(), wcre.getResponseBodyAsString());
                    } else {
                        logger.error("Authz service unexpected error for new role: {} updated by: {} for userId: {}",
                                req.newRole(), req.updatedBy(), req.userId(), ex);
                    }
                })
                .doOnSuccess(response -> {
                    String traceId = tracer.currentSpan() != null ?
                            tracer.currentSpan().context().traceId() : "no-trace";
                    logger.info("Authz response received with trace [{}] for new role: {} updated by: {} for userId: {}",
                            traceId, req.newRole(), req.updatedBy(), req.userId());
                });
    }
}