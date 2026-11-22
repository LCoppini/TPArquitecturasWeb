package com.ticketya.preferencias.service;

import com.ticketya.preferencias.dto.request.ArtistaCompraRequest;
import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.dto.response.CompraRegistradaResponse;
import com.ticketya.preferencias.repository.PreferenciaClienteRepository;
import com.ticketya.preferencias.service.impl.PreferenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciaServiceImplTest {

    private PreferenciaClienteRepository repository;
    private PreferenciaService service;

    @BeforeEach
    void setUp() {
        repository = mock(PreferenciaClienteRepository.class);
        service = new PreferenciaServiceImpl(repository);
    }

    @Test
    void primeraCompraSeRegistraYRespondeComoNueva() {
        CompraRegistradaRequest compra = compra(101L, "Tandil");
        when(repository.existeCompra(42L, 101L)).thenReturn(false);

        CompraRegistradaResponse respuesta = service.registrarCompra(compra);

        assertFalse(respuesta.yaProcesada());
        assertEquals(101L, respuesta.idEntrada());
        verify(repository).registrarCompra(compra);
    }

    @Test
    void reintentoConLaMismaEntradaSigueLlamandoAlUpdateIdempotente() {
        CompraRegistradaRequest compra = compra(101L, "Tandil");
        when(repository.existeCompra(42L, 101L)).thenReturn(true);

        CompraRegistradaResponse respuesta = service.registrarCompra(compra);

        assertTrue(respuesta.yaProcesada());
        verify(repository).registrarCompra(compra);
    }

    @Test
    void elServicioNoFiltraArtistasRepetidosNiCambiaLaCiudadDelContrato() {
        CompraRegistradaRequest compra = new CompraRegistradaRequest(42L, 101L, 20L, "SHOW", "Buenos Aires",
                List.of(new ArtistaCompraRequest(7L, "Banda"),
                        new ArtistaCompraRequest(7L, "Banda")));
        when(repository.existeCompra(42L, 101L)).thenReturn(false);

        service.registrarCompra(compra);

        ArgumentCaptor<CompraRegistradaRequest> captor = ArgumentCaptor.forClass(CompraRegistradaRequest.class);
        verify(repository).registrarCompra(captor.capture());
        assertEquals("Buenos Aires", captor.getValue().ciudad());
        assertEquals(2, captor.getValue().artistas().size());
        assertEquals(7L, captor.getValue().artistas().get(0).idArtista());
        assertEquals(7L, captor.getValue().artistas().get(1).idArtista());
    }

    @Test
    void obtenerConvierteElDocumentoEnDtoSinExponerLaEntidad() {
        com.ticketya.preferencias.entity.PreferenciaCliente documento =
                new com.ticketya.preferencias.entity.PreferenciaCliente(42L);
        documento.setCiudad("Tandil");
        documento.getTiposEvento().put("SHOW", 3);
        documento.getArtistas().add(new com.ticketya.preferencias.entity.ArtistaPreferido(7L, "Banda", 2));
        documento.getComprasProcesadas().add(101L);
        documento.getEventosComprados().add(20L);
        when(repository.findById(42L)).thenReturn(java.util.Optional.of(documento));

        com.ticketya.preferencias.dto.response.PreferenciaClienteResponse respuesta = service.obtener(42L);

        assertEquals(42L, respuesta.idUsuario());
        assertEquals("Tandil", respuesta.ciudad());
        assertEquals(3, respuesta.tiposEvento().get("SHOW"));
        assertEquals(1, respuesta.artistas().size());
        assertEquals(7L, respuesta.artistas().get(0).idArtista());
        assertEquals("Banda", respuesta.artistas().get(0).nombre());
        assertEquals(2, respuesta.artistas().get(0).cantidad());
    }

    @Test
    void obtenerSinDocumentoDevuelve404() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.obtener(99L));
    }

    private CompraRegistradaRequest compra(Long idEntrada, String ciudad) {
        return new CompraRegistradaRequest(42L, idEntrada, 20L, "SHOW", ciudad,
                List.of(new ArtistaCompraRequest(7L, "Banda")));
    }
}
