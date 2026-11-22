package com.ticketya.notificaciones.dto;

import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudNotificacionRequestValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    private static Set<String> campos(SolicitudNotificacionRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static SolicitudNotificacionRequest conClave(String clave) {
        return new SolicitudNotificacionRequest(clave, 42L, TipoNotificacion.COMPRA_CONFIRMADA, null, Map.of());
    }

    @Test
    void unaSolicitudCompletaEsValida() {
        SolicitudNotificacionRequest request = new SolicitudNotificacionRequest("k1", 42L,
                TipoNotificacion.EVENTO_CANCELADO, "ana@correo.com", Map.of("evento", "Ritual"));

        assertTrue(campos(request).isEmpty());
    }

    @Test
    void elDestinatarioEsOpcional() {
        assertTrue(campos(conClave("k1")).isEmpty());
    }

    @Test
    void laClaveEsObligatoria() {
        assertEquals(Set.of("claveIdempotencia"), campos(conClave("  ")));
        assertEquals(Set.of("claveIdempotencia"), campos(conClave(null)));
    }

    @Test
    void laClaveNoPuedeSuperarLos120Caracteres() {
        assertEquals(Set.of("claveIdempotencia"), campos(conClave("x".repeat(121))));
    }

    @Test
    void elUsuarioYElTipoSonObligatorios() {
        SolicitudNotificacionRequest request = new SolicitudNotificacionRequest("k1", null, null, null, null);

        assertEquals(Set.of("idUsuario", "tipo"), campos(request));
    }

    @Test
    void elDestinatarioDebeSerUnEmailValido() {
        SolicitudNotificacionRequest request = new SolicitudNotificacionRequest("k1", 42L,
                TipoNotificacion.COMPRA_CONFIRMADA, "no-es-un-email", null);

        assertEquals(Set.of("destinatario"), campos(request));
    }
}
