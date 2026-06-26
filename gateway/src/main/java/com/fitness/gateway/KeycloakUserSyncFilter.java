package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String token =
                exchange.getRequest().getHeaders().getFirst("Authorization");

        String userId =
                exchange.getRequest().getHeaders().getFirst("X-User-ID");

        log.info("========================================");
        log.info("Incoming Request: {}", exchange.getRequest().getURI());
        log.info("Authorization Header: {}", token);
        log.info("X-User-ID Header: {}", userId);
        log.info("========================================");

        RegisterRequest registerRequest = getUserDetails(token);

        if (registerRequest != null) {
            log.info("Extracted User Details:");
            log.info("Keycloak ID : {}", registerRequest.getKeycloakId());
            log.info("Email       : {}", registerRequest.getEmail());
            log.info("First Name  : {}", registerRequest.getFirstName());
            log.info("Last Name   : {}", registerRequest.getLastName());
        } else {
            log.error("RegisterRequest is NULL");
        }

        if (userId == null && registerRequest != null) {
            userId = registerRequest.getKeycloakId();
            log.info("Using Keycloak ID as User ID: {}", userId);
        }

        if (userId != null && token != null) {

            String finalUserId = userId;

            return userService.validateUser(userId)
                    .doOnNext(exists ->
                            log.info("User Exists in DB ? {}", exists))
                    .flatMap(exists -> {

                        if (!exists) {

                            log.info("User not found. Registering user...");

                            if (registerRequest != null) {

                                return userService.registerUser(registerRequest)
                                        .doOnSuccess(user ->
                                                log.info("User Registered Successfully"))
                                        .doOnError(error ->
                                                log.error("Registration Failed", error))
                                        .then();

                            } else {

                                log.error("Cannot register because RegisterRequest is null");

                                return Mono.empty();
                            }

                        } else {

                            log.info("User already exists. Skipping registration.");
                            return Mono.empty();
                        }
                    })
                    .then(Mono.defer(() -> {

                        log.info("Adding X-User-ID Header: {}", finalUserId);

                        ServerHttpRequest mutatedRequest =
                                exchange.getRequest()
                                        .mutate()
                                        .header("X-User-ID", finalUserId)
                                        .build();

                        return chain.filter(
                                exchange.mutate()
                                        .request(mutatedRequest)
                                        .build()
                        );
                    }));
        }

        log.warn("Skipping sync because token or userId is null");
        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {

        try {

            if (token == null || token.isBlank()) {
                log.error("Authorization Header is missing");
                return null;
            }

            String tokenWithoutBearer =
                    token.replace("Bearer ", "").trim();

            log.info("Parsing JWT Token...");

            SignedJWT signedJWT =
                    SignedJWT.parse(tokenWithoutBearer);

            JWTClaimsSet claims =
                    signedJWT.getJWTClaimsSet();

            log.info("========================================");
            log.info("JWT Claims:");
            log.info("{}", claims.toJSONObject());
            log.info("========================================");

            RegisterRequest registerRequest =
                    new RegisterRequest();

            registerRequest.setEmail(
                    claims.getStringClaim("email"));

            registerRequest.setKeycloakId(
                    claims.getStringClaim("sub"));

            registerRequest.setFirstName(
                    claims.getStringClaim("given_name"));

            registerRequest.setLastName(
                    claims.getStringClaim("family_name"));

            registerRequest.setPassword("dummy@123123");

            log.info("SUB Claim: {}", claims.getStringClaim("sub"));

            return registerRequest;

        } catch (Exception e) {

            log.error("Failed to Parse JWT", e);

            return null;
        }
    }
}