package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilaBloqueoIndividualTest {

    @Mock private EntradaRepository entradaRepository;
    @Mock private FilaEventoRepository filaEventoRepository;
    @Mock private UsuarioClient usuarioClient;
    @Mock private EventoClient eventoClient;
    @Mock private SectorClient sectorClient;
    @Mock private PagoRepository pagoRepository;
    @Mock private RegistroPagoRepository registroPagoRepository;

    private EntradaService entradaService;
    private PagoService pagoService;

    @BeforeEach
    void configurar() {
        entradaService = new EntradaService(
                entradaRepository, filaEventoRepository, usuarioClient, eventoClient, sectorClient, 6
        );
        pagoService = new PagoService(
                pagoRepository, entradaRepository, filaEventoRepository, registroPagoRepository, "APRUEBA"
        );
    }

    @ParameterizedTest(name = "fila {0}")
    @ValueSource(strings = {"ACTIVA", "PAUSADA", "AGOTADA", "CERRADA"})
    void reservaIndividualSeBloqueaEnCualquierEstadoPersistido(String estado) {
        when(entradaRepository.bloquearSolicitudIdempotente(any())).thenReturn(1);
        when(entradaRepository.findByIdUsuarioAndClaveIdempotencia(10L, "reserva-1"))
                .thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L))
                .thenReturn(new EventoResponse(20L, Instant.now().plusSeconds(86_400), "PUBLICADO"));
        when(eventoClient.obtenerPrecioSector(20L, 30L))
                .thenReturn(new EventoSectorResponse(30L, new BigDecimal("250.00")));
        when(sectorClient.obtenerPorId(30L)).thenReturn(new SectorResponse(30L, 30, 3, 10, "NUMERADO"));
        when(filaEventoRepository.existsById(20L)).thenReturn(true);
        FilaEvento fila = new FilaEvento();
        fila.setEstado(estado);
        lenient().when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> entradaService.crear(solicitudReserva()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(entradaRepository, never()).save(any(Entrada.class));
        verify(filaEventoRepository, never()).buscarPorEventoConBloqueo(20L);
        verify(filaEventoRepository, never()).findById(20L);
    }

    @ParameterizedTest(name = "fila {0}")
    @ValueSource(strings = {"ACTIVA", "PAUSADA", "AGOTADA", "CERRADA"})
    void pagoIndividualSeBloqueaEnCualquierEstadoPersistido(String estado) {
        Entrada reserva = new Entrada();
        reserva.setId(100L);
        reserva.setIdEvento(20L);
        reserva.setIdUsuario(10L);
        reserva.setEstado("RESERVADA");
        when(entradaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(filaEventoRepository.existsById(20L)).thenReturn(true);
        FilaEvento fila = new FilaEvento();
        fila.setEstado(estado);
        lenient().when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> pagoService.crear(solicitudPago()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(entradaRepository, never()).buscarPorIdConBloqueo(100L);
        verify(filaEventoRepository, never()).buscarPorEventoConBloqueo(20L);
        verify(filaEventoRepository, never()).findById(20L);
    }

    @Test
    void sinFilaPersistidaLaReservaIndividualSigueDisponible() {
        when(entradaRepository.bloquearSolicitudIdempotente(any())).thenReturn(1);
        when(entradaRepository.findByIdUsuarioAndClaveIdempotencia(10L, "reserva-1"))
                .thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L))
                .thenReturn(new EventoResponse(20L, Instant.now().plusSeconds(86_400), "PUBLICADO"));
        when(eventoClient.obtenerPrecioSector(20L, 30L))
                .thenReturn(new EventoSectorResponse(30L, new BigDecimal("250.00")));
        when(sectorClient.obtenerPorId(30L)).thenReturn(new SectorResponse(30L, 30, 3, 10, "NUMERADO"));
        when(filaEventoRepository.existsById(20L)).thenReturn(false);
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = entradaService.crear(solicitudReserva());

        assertEquals(2, respuesta.getFila());
        assertEquals(4, respuesta.getAsiento());
    }

    private EntradaRequest solicitudReserva() {
        EntradaRequest request = new EntradaRequest();
        request.setIdEvento(20L);
        request.setIdUsuario(10L);
        request.setIdSector(30L);
        request.setPrecio(new BigDecimal("100.00"));
        request.setFila(2);
        request.setAsiento(4);
        request.setClaveIdempotencia("reserva-1");
        return request;
    }

    private PagoRequest solicitudPago() {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(100L);
        request.setIdUsuario(10L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("pago-1");
        return request;
    }
}
