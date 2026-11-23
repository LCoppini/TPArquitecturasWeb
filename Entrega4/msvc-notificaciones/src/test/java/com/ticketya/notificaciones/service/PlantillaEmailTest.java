package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.service.impl.PlantillaEmail;
import com.ticketya.notificaciones.service.impl.PlantillaEmail.EmailRenderizado;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantillaEmailTest {

    private final PlantillaEmail plantilla = new PlantillaEmail();

    private static Map<String, String> datosCompra() {
        Map<String, String> datos = new HashMap<>();
        datos.put("evento", "Ritual en vivo");
        datos.put("fechaHora", "24/10 21:00");
        datos.put("idEntrada", "101");
        return datos;
    }

    @Test
    void faltantesIndicaLosDatosObligatoriosQueNoVienen() {
        Map<String, String> datos = new HashMap<>();
        datos.put("evento", "Ritual en vivo");

        List<String> faltantes = plantilla.faltantes(TipoNotificacion.COMPRA_CONFIRMADA, datos);

        assertEquals(List.of("fechaHora", "idEntrada"), faltantes);
    }

    @Test
    void faltantesConsideraVaciosLosValoresEnBlanco() {
        Map<String, String> datos = datosCompra();
        datos.put("idEntrada", "   ");

        assertEquals(List.of("idEntrada"), plantilla.faltantes(TipoNotificacion.COMPRA_CONFIRMADA, datos));
    }

    @Test
    void faltantesAceptaDatosNulos() {
        assertEquals(List.of("evento", "cambios"), plantilla.faltantes(TipoNotificacion.EVENTO_MODIFICADO, null));
    }

    @Test
    void sinFaltantesCuandoVienenTodosLosDatos() {
        assertTrue(plantilla.faltantes(TipoNotificacion.COMPRA_CONFIRMADA, datosCompra()).isEmpty());
    }

    @Test
    void compraConfirmadaIncluyeEventoFechaYEntrada() {
        EmailRenderizado email = plantilla.renderizar(TipoNotificacion.COMPRA_CONFIRMADA, datosCompra());

        assertEquals("Tu compra para Ritual en vivo está confirmada", email.asunto());
        assertTrue(email.cuerpo().contains("Evento: Ritual en vivo"));
        assertTrue(email.cuerpo().contains("Fecha y hora: 24/10 21:00"));
        assertTrue(email.cuerpo().contains("Entrada Nº: 101"));
    }

    @Test
    void elSaludoUsaElNombreSiViene() {
        Map<String, String> datos = datosCompra();
        datos.put("nombre", "Ana");

        assertTrue(plantilla.renderizar(TipoNotificacion.COMPRA_CONFIRMADA, datos).cuerpo().startsWith("Hola Ana,"));
    }

    @Test
    void elSaludoEsGenericoSinNombre() {
        assertTrue(plantilla.renderizar(TipoNotificacion.COMPRA_CONFIRMADA, datosCompra()).cuerpo().startsWith("Hola,"));
    }

    @Test
    void losCaracteresEspecialesDelDatoSeCopianTalCual() {
        Map<String, String> datos = datosCompra();
        datos.put("evento", "Tributo a $uper Band \\ 2x1");

        EmailRenderizado email = plantilla.renderizar(TipoNotificacion.COMPRA_CONFIRMADA, datos);

        assertTrue(email.cuerpo().contains("Evento: Tributo a $uper Band \\ 2x1"));
    }

    @Test
    void todosLosTiposSeRenderizanSinMarcadoresSinReemplazar() {
        Map<String, String> datos = new HashMap<>();
        for (String clave : List.of("evento", "fechaHora", "idEntrada", "monto", "cambios", "artista", "ciudad")) {
            datos.put(clave, clave + "-valor");
        }

        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            EmailRenderizado email = plantilla.renderizar(tipo, datos);

            assertFalse(email.asunto().contains("{"), "asunto de " + tipo);
            assertFalse(email.cuerpo().contains("{"), "cuerpo de " + tipo);
            assertTrue(plantilla.faltantes(tipo, datos).isEmpty(), "datos de " + tipo);
        }
    }

    @Test
    void unDatoOpcionalAusenteSeReemplazaPorGuion() {
        Map<String, String> datos = new HashMap<>();
        datos.put("evento", "Ritual en vivo");
        datos.put("fechaHora", "24/10 21:00");

        EmailRenderizado email = plantilla.renderizar(TipoNotificacion.COMPRA_CONFIRMADA, datos);

        assertTrue(email.cuerpo().contains("Entrada Nº: -"));
    }
}
