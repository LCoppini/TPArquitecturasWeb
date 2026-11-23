package com.ticketya.gateway.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Matriz rol -> path aprobada para Entrega 2 (ver design.md).
 *
 * <p>Los authorities tienen el prefijo ROLE_, por eso se usa hasRole(...).
 * Las reglas publicas (login, registro, GET de eventos y artistas) se evaluan
 * antes de esta matriz en SecurityConfig; los demas metodos sobre esos prefijos
 * se deniegan aqui para que la autorizacion no dependa del fallback.
 */
@Component
public class GatewayRoleMatrix {

    private static final String[] ADMIN = {
            "/api/usuarios/**", "/api/roles/**", "/api/admin/usuarios/**",
            "/api/admin/preferencias/**", "/api/admin/recomendaciones/**"
    };
    private static final String[] ADMIN_Y_ORGANIZADOR = {
            "/api/lugares/**", "/api/sectores/**"
    };
    private static final String[] SOLO_LECTURA = {
            "/api/eventos/**", "/api/artistas/**"
    };
    private static final String[] ORGANIZADOR = {
            "/api/organizador/eventos/**", "/api/organizador/filas-virtuales/**"
    };
    private static final String[] ADMIN_ESCRITURA = {
            "/api/admin/eventos/**", "/api/admin/artistas/**"
    };
    private static final String[] CLIENTE = {
            "/api/entradas/**", "/api/pagos/**", "/api/filas-virtuales/**",
            "/api/compras/**", "/api/preferencias/**", "/api/recomendaciones/**"
    };
    private static final String[] ADMIN_ENTRADAS = {
            "/api/admin/entradas/**"
    };
    private static final String[] PERSONAL_ACCESO = {
            "/api/accesos/**"
    };

    /**
     * Prefijos que tienen regla explicita. La prueba de matriz los usa para
     * fallar si alguien agrega una ruta al gateway sin decidir su rol.
     */
    public static List<String> prefijosConRegla() {
        return Stream.of(ADMIN, ADMIN_Y_ORGANIZADOR, SOLO_LECTURA, ORGANIZADOR,
                        ADMIN_ESCRITURA, CLIENTE, ADMIN_ENTRADAS, PERSONAL_ACCESO)
                .flatMap(Arrays::stream)
                .map(patron -> patron.substring(0, patron.length() - 3))
                .distinct()
                .toList();
    }

    public void apply(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {

        authz.requestMatchers(ADMIN)
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers(ADMIN_Y_ORGANIZADOR)
                .hasAnyRole(GatewayRoles.ADMINISTRADOR, GatewayRoles.ORGANIZADOR);

        // Solo GET es publico (SecurityConfig). Escritura va por /api/admin/** o
        // /api/organizador/**, nunca por el prefijo de consulta.
        authz.requestMatchers(SOLO_LECTURA)
                .denyAll();

        authz.requestMatchers(ORGANIZADOR)
                .hasRole(GatewayRoles.ORGANIZADOR);

        authz.requestMatchers(ADMIN_ESCRITURA)
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers(CLIENTE)
                .hasRole(GatewayRoles.CLIENTE);

        authz.requestMatchers(ADMIN_ENTRADAS)
                .hasRole(GatewayRoles.ADMINISTRADOR);

        authz.requestMatchers(PERSONAL_ACCESO)
                .hasRole(GatewayRoles.PERSONAL_ACCESO);
    }
}
