package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Salida voluntaria de la fila y espera estimada del cliente en espera. */
@ExtendWith(MockitoExtension.class)
class FilaVirtualSalidaTest {

    @Mock
    private FilaEventoRepository filaEventoRepository;
    @Mock
    private IngresoFilaRepository ingresoFilaRepository;
    @Mock
    private TandaFilaRepository tandaFilaRepository;
    @Mock
    private EntradaRepository entradaRepository;
    @Mock
    private EventoClient eventoClient;
    @Mock
    private SectorClient sectorClient;
    @Mock
    private CompraService compraService;

    private FilaVirtualService service;

    @BeforeEach
    void setUp() {
        service = new FilaVirtualService(filaEventoRepository, ingresoFilaRepository, tandaFilaRepository,
                entradaRepository, eventoClient, sectorClient, compraService);
    }

    @Test
    void salirDeLaFilaEnEsperaMarcaElIngresoComoSalio() {
        IngresoFila ingreso = ingreso("EN_ESPERA", 3L);
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.of(ingreso));

        var respuesta = service.salir(20L, 10L);

        assertEquals("SALIO", ingreso.getEstado());
        assertEquals("SALIO", respuesta.estado());
        assertNull(respuesta.posicion());
    }

    @Test
    void salirDosVecesEsIdempotente() {
        IngresoFila ingreso = ingreso("SALIO", 3L);
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.of(ingreso));

        assertEquals("SALIO", service.salir(20L, 10L).estado());
    }

    @Test
    void salirHabilitadoResponde409() {
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.of(ingreso("HABILITADO", 1L)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.salir(20L, 10L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void salirSinIngresoLanzaNoEncontrado() {
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.salir(20L, 10L));
    }

    @Test
    void miEstadoEnEsperaIncluyeEsperaEstimadaSegunTandasPrevias() {
        IngresoFila ingreso = ingreso("EN_ESPERA", 5L);
        TandaFila abierta = new TandaFila();
        abierta.setVenceEn(Instant.now().plus(5, ChronoUnit.MINUTES));
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.of(ingreso));
        when(ingresoFilaRepository.countByIdEventoAndEstadoAndSecuenciaLessThan(20L, "EN_ESPERA", 5L))
                .thenReturn(4L);
        when(tandaFilaRepository.findFirstByIdEventoAndEstado(20L, "ABIERTA")).thenReturn(Optional.of(abierta));

        var respuesta = service.consultarMiEstado(20L, 10L);

        // posicion 5 con maxPersonas 2 => 2 tandas previas (1200 s) + lo que resta de la abierta (~300 s)
        assertEquals(5L, respuesta.posicion());
        assertNotNull(respuesta.esperaEstimadaSegundos());
        assertTrue(respuesta.esperaEstimadaSegundos() > 1400 && respuesta.esperaEstimadaSegundos() <= 1500);
    }

    @Test
    void sinTandaAbiertaNoHayEsperaEstimada() {
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila()));
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 10L))
                .thenReturn(Optional.of(ingreso("EN_ESPERA", 2L)));
        when(ingresoFilaRepository.countByIdEventoAndEstadoAndSecuenciaLessThan(20L, "EN_ESPERA", 2L))
                .thenReturn(1L);
        when(tandaFilaRepository.findFirstByIdEventoAndEstado(20L, "ABIERTA")).thenReturn(Optional.empty());

        assertNull(service.consultarMiEstado(20L, 10L).esperaEstimadaSegundos());
    }

    private IngresoFila ingreso(String estado, Long secuencia) {
        IngresoFila ingreso = new IngresoFila();
        ingreso.setIdEvento(20L);
        ingreso.setIdUsuario(10L);
        ingreso.setSecuencia(secuencia);
        ingreso.setEstado(estado);
        return ingreso;
    }

    private FilaEvento fila() {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");
        fila.setMaxPersonas(2);
        fila.setCupoEntradas(4);
        fila.setCapacidadTotal(10);
        fila.setSiguienteSecuencia(6L);
        return fila;
    }
}
