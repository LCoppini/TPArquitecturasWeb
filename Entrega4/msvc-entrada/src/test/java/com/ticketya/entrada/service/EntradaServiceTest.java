package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private EventoClient eventoClient;

    @Mock
    private SectorClient sectorClient;

    private EntradaService entradaService;

    @BeforeEach
    void setUp() {
        entradaService = new EntradaService(
                entradaRepository,
                usuarioClient,
                eventoClient,
                sectorClient,
                6
        );
    }

    @Test
    void creaReservaNumeradaYGuardaFilaYAsiento() {
        // Verifica que la selección válida del sector se persista y se devuelva al cliente.
        prepararReservaNumerada(0);
        EntradaRequest request = crearSolicitud(2, 4);
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EntradaResponse respuesta = entradaService.crear(request);

        ArgumentCaptor<Entrada> captor = ArgumentCaptor.forClass(Entrada.class);
        verify(entradaRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getFila());
        assertEquals(4, captor.getValue().getAsiento());
        assertEquals(new BigDecimal("250.00"), captor.getValue().getPrecio());
        assertEquals(2, respuesta.getFila());
        assertEquals(4, respuesta.getAsiento());
        assertEquals(new BigDecimal("250.00"), respuesta.getPrecio());
    }

    @Test
    void rechazaReservaCuandoElAsientoYaEstaOcupado() {
        // Evita asignar el mismo asiento si ya existe una reserva vigente o una entrada emitida.
        prepararReservaNumerada(1);

        assertThrows(IllegalStateException.class, () -> entradaService.crear(crearSolicitud(2, 4)));

        verify(entradaRepository, never()).save(any(Entrada.class));
    }

    @Test
    void rechazaReservaNumeradaSinFilaOAsiento() {
        // Exige la selección completa porque un sector numerado no puede reservarse sin asiento concreto.
        prepararReservaNumerada(0);
        EntradaRequest request = crearSolicitud(null, null);

        assertThrows(IllegalArgumentException.class, () -> entradaService.crear(request));

        verify(entradaRepository, never()).save(any(Entrada.class));
    }

    @Test
    void rechazaAsientoFueraDeLasDimensionesDelSector() {
        // Comprueba los límites de filas y asientos recibidos desde msvc-ubicacion.
        prepararReservaNumerada(0);

        assertThrows(IllegalArgumentException.class, () -> entradaService.crear(crearSolicitud(4, 4)));

        verify(entradaRepository, never()).save(any(Entrada.class));
    }

    @Test
    void rechazaFilaYAsientoEnSectorNoNumerado() {
        // Los sectores sin numeración aceptan capacidad general, no una asignación individual de asiento.
        when(entradaRepository.bloquearSolicitudIdempotente(any())).thenReturn(1);
        when(entradaRepository.findByIdUsuarioAndClaveIdempotencia(10L, "reserva-1"))
                .thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado());
        when(eventoClient.obtenerPrecioSector(20L, 30L))
                .thenReturn(new EventoSectorResponse(30L, new BigDecimal("250.00")));
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 30, null, null, "NO_NUMERADO")
        );

        assertThrows(IllegalArgumentException.class, () -> entradaService.crear(crearSolicitud(2, 4)));

        verify(entradaRepository, never()).save(any(Entrada.class));
    }

    private void prepararReservaNumerada(long asientoOcupado) {
        when(entradaRepository.bloquearSolicitudIdempotente(any())).thenReturn(1);
        when(entradaRepository.findByIdUsuarioAndClaveIdempotencia(10L, "reserva-1"))
                .thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado());
        when(eventoClient.obtenerPrecioSector(20L, 30L))
                .thenReturn(new EventoSectorResponse(30L, new BigDecimal("250.00")));
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 30, 3, 10, "NUMERADO")
        );
        lenient().when(entradaRepository.bloquearReservasPorClave("20:30")).thenReturn(1);
        lenient().when(entradaRepository.contarActivasDeSector(eq(20L), eq(30L), any(Instant.class))).thenReturn(0L);
        lenient().when(entradaRepository.contarActivasDeUsuario(eq(20L), eq(10L), any(Instant.class)))
                .thenReturn(0L);
        lenient().when(entradaRepository.contarAsignacionesActivasDeAsiento(
                eq(20L), eq(30L), eq(2), eq(4), any(Instant.class)
        )).thenReturn(asientoOcupado);
    }

    private EventoResponse eventoPublicado() {
        return new EventoResponse(20L, Instant.now().plusSeconds(86_400), "PUBLICADO");
    }

    private EntradaRequest crearSolicitud(Integer fila, Integer asiento) {
        EntradaRequest request = new EntradaRequest();
        request.setIdEvento(20L);
        request.setIdUsuario(10L);
        request.setIdSector(30L);
        request.setPrecio(new BigDecimal("100.00"));
        request.setFila(fila);
        request.setAsiento(asiento);
        request.setClaveIdempotencia("reserva-1");
        return request;
    }
}
