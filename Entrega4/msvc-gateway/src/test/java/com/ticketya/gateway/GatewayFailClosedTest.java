package com.ticketya.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El gateway es fail-closed: sin clave publica, audiencia e issuer no puede
 * construir el JwtDecoder y, por lo tanto, el contexto no arranca. Nunca cae
 * en la identidad simulada de Entrega 1.
 *
 * <p>Se usa un {@link MockEnvironment} para que la prueba no dependa de las
 * variables de entorno del proceso que ejecuta Maven.
 */
class GatewayFailClosedTest {

    @Test
    void sinVariablesJwtElContextoNoArranca() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.cloud.config.enabled", "false");
        environment.setProperty("eureka.client.enabled", "false");
        environment.setProperty("server.port", "0");

        SpringApplication application = new SpringApplication(MsvcGatewayApplication.class);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setEnvironment(environment);

        Exception fallo = assertThrows(Exception.class, application::run);

        assertTrue(mensajesDeCausas(fallo).stream().anyMatch(this::esFalloDeConfiguracionJwt),
                () -> "El contexto falló, pero no por la configuración JWT: " + fallo);
    }

    private boolean esFalloDeConfiguracionJwt(String mensaje) {
        return mensaje.contains("JWT_PUBLIC_KEY_LOCATION")
                || mensaje.contains("JWT_AUDIENCE")
                || mensaje.contains("JWT_ISSUER")
                || mensaje.contains("public-key-location")
                || mensaje.contains("JwtDecoder");
    }

    private List<String> mensajesDeCausas(Throwable fallo) {
        List<String> mensajes = new ArrayList<>();
        Throwable actual = fallo;
        while (actual != null) {
            if (actual.getMessage() != null) {
                mensajes.add(actual.getMessage());
            }
            actual = actual.getCause();
        }
        return mensajes;
    }
}
