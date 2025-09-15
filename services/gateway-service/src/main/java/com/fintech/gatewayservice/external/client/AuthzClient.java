package com.fintech.gatewayservice.external.client;

import com.fintech.gatewayservice.external.model.request.AuthzIntrospectRequest;
import com.fintech.gatewayservice.external.model.response.AuthzIntrospectResponse;
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

    @Value("${gateway.authz.base-url}")
    private String authzBase;
    @Value("${gateway.authz.timeout-ms:1200}")
    private int timeoutMs;

    public AuthzClient(WebClient authzWebClient, Tracer tracer) {
        this.webClient = authzWebClient;
        this.tracer = tracer;
    }

    public Mono<AuthzIntrospectResponse> introspect(AuthzIntrospectRequest req) {

        return Mono.fromCallable(() -> {
                    // Log current trace context
                    String traceId = tracer.currentSpan() != null ?
                            tracer.currentSpan().context().traceId() : "no-trace";
                    String spanId = tracer.currentSpan() != null ?
                            tracer.currentSpan().context().spanId() : "no-span";

                    logger.info("Making authz call with trace [{}] span [{}] for path={} method={}",
                            traceId, spanId, req.path, req.method);
                    return req;
                })
                .flatMap(request ->
                        webClient.post()
                                .uri(authzBase + "/api/authz/introspect")
                                .bodyValue(req)
                                .retrieve()
                                .bodyToMono(AuthzIntrospectResponse.class)
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
                .doOnSuccess(response -> {
                    String traceId = tracer.currentSpan() != null ?
                            tracer.currentSpan().context().traceId() : "no-trace";
                    logger.info("Authz response received with trace [{}] for path={} method={} allowed={}",
                            traceId, req.path, req.method, response != null ? response.allowed : "null");
                });
    }
}