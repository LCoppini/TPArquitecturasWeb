package com.ticketya.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Cadena de seguridad del gateway MVC (servlet): valida JWT RS256 y autoriza por rol. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String MENSAJE_401 = "Token ausente, invalido o vencido.";
    private static final String MENSAJE_403 = "No tenes el rol requerido para este recurso.";

    /**
     * El claim "roles" se mapea a authorities ROLE_*; la identidad se toma de
     * "idUsuario" (no del subject) porque ese es el contrato del token emitido
     * por msvc-usuarios. Ver ADR-0001.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("idUsuario");
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GatewayRoleMatrix roleMatrix,
            GatewaySecurityErrorWriter writer,
            JwtAuthenticationConverter converter
    ) throws Exception {

        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> {
                    // El login debe ir antes de la regla general de /api/usuarios/**.
                    authz.requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll();
                    roleMatrix.apply(authz);
                    authz.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) ->
                                writer.write(res, req, HttpStatus.UNAUTHORIZED, MENSAJE_401))
                        .accessDeniedHandler((req, res, ex) ->
                                writer.write(res, req, HttpStatus.FORBIDDEN, MENSAJE_403))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) ->
                                writer.write(res, req, HttpStatus.UNAUTHORIZED, MENSAJE_401))
                        .accessDeniedHandler((req, res, authEx) ->
                                writer.write(res, req, HttpStatus.FORBIDDEN, MENSAJE_403))
                );

        return http.build();
    }
}
