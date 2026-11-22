package com.ticketya.preferencias.entity;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenciaClienteMappingTest {

    @Test
    void documentoUsaLaColeccionAcordadaYIdEsElUsuario() throws Exception {
        Document document = PreferenciaCliente.class.getAnnotation(Document.class);
        assertNotNull(document);
        assertEquals("preferencias_cliente", document.collection());

        Field id = PreferenciaCliente.class.getDeclaredField("idUsuario");
        assertNotNull(id.getAnnotation(Id.class));
        assertEquals("_id", id.getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class).value());
    }

    @Test
    void elDocumentoSoloGuardaIdentificadoresYTiposPropios() throws Exception {
        assertEquals(String.class, tipoDelCampo("ciudad"));
        assertEquals(Map.class, tipoDelCampo("tiposEvento"));
        assertEquals(List.class, tipoDelCampo("artistas"));
        assertEquals(Set.class, tipoDelCampo("comprasProcesadas"));
        assertEquals(Set.class, tipoDelCampo("eventosComprados"));
        assertEquals(Instant.class, tipoDelCampo("actualizadoEn"));

        assertEquals(Long.class, ArtistaPreferido.class.getDeclaredField("idArtista").getType());
        assertEquals(String.class, ArtistaPreferido.class.getDeclaredField("nombre").getType());
        assertEquals(int.class, ArtistaPreferido.class.getDeclaredField("cantidad").getType());

        for (Field campo : PreferenciaCliente.class.getDeclaredFields()) {
            assertFalse(campo.getType().getName().contains("entrada"),
                    "El documento no debe guardar entidades de msvc-entrada");
            assertFalse(campo.getType().getName().contains("evento.entity"),
                    "El documento no debe guardar entidades de msvc-evento");
            assertFalse(campo.getType().getName().contains("usuario.entity"),
                    "El documento no debe guardar entidades de msvc-usuarios");
        }
    }

    @Test
    void coleccionesVienenInicializadasYEspecificanEventosComprados() throws Exception {
        PreferenciaCliente preferencia = new PreferenciaCliente(42L);

        assertEquals(42L, preferencia.getIdUsuario());
        assertNotNull(preferencia.getTiposEvento());
        assertNotNull(preferencia.getArtistas());
        assertNotNull(preferencia.getComprasProcesadas());
        assertNotNull(preferencia.getEventosComprados());
        assertTrue(preferencia.getEventosComprados().isEmpty());

        Field eventosComprados = PreferenciaCliente.class.getDeclaredField("eventosComprados");
        assertEquals("eventos_comprados",
                eventosComprados.getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class).value());
    }

    private Class<?> tipoDelCampo(String nombre) throws Exception {
        return PreferenciaCliente.class.getDeclaredField(nombre).getType();
    }
}
