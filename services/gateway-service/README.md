# Gateway Service

## Overview
This service acts as the API gateway for the fintech platform, handling authentication, rate limiting, routing, and security for all incoming requests.

## Key Features
- JWT authentication and validation
- Per-IP distributed rate limiting using Redis
- Centralized configuration via Spring Cloud Config Server
- Observability: Prometheus metrics, tracing, and logging
- Health checks via Spring Boot Actuator

## Security
- All endpoints except `/api/auth/**` require JWT authentication
- Rate limiting is enforced per IP address
- Sensitive endpoints are protected and actuator endpoints should be restricted in production
- Security headers are recommended for all responses

## Configuration
- All sensitive values (JWT cert path, Redis host/port) should be externalized via config server or environment variables
- CORS configuration should be set to allow only required origins

## Health
- `/actuator/health` endpoint checks Redis and JWT key availability
- Custom health indicators can be added for config server connectivity

## Testing
- Unit and integration tests should cover JWT validation, rate limiting, and error handling

## Improvements
- Add request/response logging for audit and debugging
- Add OpenAPI/Swagger documentation for routing reference
- Tune Redisson and Resilience4j for performance

## Contact
For questions or issues, contact the platform engineering team.
