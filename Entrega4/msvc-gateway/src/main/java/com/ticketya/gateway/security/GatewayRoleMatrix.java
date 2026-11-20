package com.ticketya.gateway.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Matriz rol -> path aprobada para Entrega 2.
 *
 * <p>Los authorities tienen el prefijo ROLE_, por eso se usa hasRole(...).
 * El login se permite en SecurityConfig antes de esta matriz. Los prefijos
 * /api/admin/** y /api/organizador/** quedan declarados para la separacion de
 * endpoints aprobada; su alineacion con los controllers esta registrada como
 * tarea pendiente en el change gateway-jwt-e2.
 */
@Component
public class GatewayRoleMatrix {

    public void apply(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {

        authz.requestMatchers("/api/usuarios/**", "/api/roles/**")
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers("/api/lugares/**", "/api/sectores/**")
                .hasAnyRole(GatewayRoles.ADMINISTRADOR, GatewayRoles.ORGANIZADOR);

        authz.requestMatchers("/api/eventos/**", "/api/artistas/**")
                .hasAnyRole(GatewayRoles.ADMINISTRADOR, GatewayRoles.ORGANIZADOR, GatewayRoles.CLIENTE);

        authz.requestMatchers("/api/organizador/eventos/**")
                .hasRole(GatewayRoles.ORGANIZADOR);

        authz.requestMatchers("/api/admin/eventos/**")
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers("/api/entradas/**", "/api/pagos/**")
                .hasAnyRole(GatewayRoles.ADMINISTRADOR, GatewayRoles.CLIENTE, GatewayRoles.PERSONAL_ACCESO);

        authz.requestMatchers("/api/admin/entradas/**")
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers("/api/accesos/**")
                .hasRole(GatewayRoles.PERSONAL_ACCESO);

        authz.requestMatchers("/api/preferencias/**", "/api/recomendaciones/**")
                .hasAnyRole(GatewayRoles.ADMINISTRADOR, GatewayRoles.CLIENTE);
    }
}
