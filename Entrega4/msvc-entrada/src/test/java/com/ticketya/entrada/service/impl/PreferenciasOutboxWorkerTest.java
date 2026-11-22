package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.config.OutboxProperties;
import com.ticketya.entrada.dto.request.CompraRegistradaRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.ParticipanteResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.service.PreferenciasGateway;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenciasOutboxWorkerTest {

    @Mock
    private PendientePreferenciaRepository repository;

    @Mock
    private EventoClient eventoClient;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private PreferenciasGateway gateway;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private PreferenciasOutboxWorker worker;

    @BeforeEach
    void setUp() {
        worker = new PreferenciasOutboxWorker(repository, eventoClient, usuarioClient,
                gateway, new OutboxProperties(), circuitBreakerRegistry);
    }

    @Test
    void envioExitosoMarcaComoEnviadoConElContratoCompleto() {
        prepararEnriquecimiento();
        doAnswer(invocacion -> {
            assertEquals("corr-worker", MDC.get(CorrelationContext.MDC_KEY));
            return null;
        }).when(gateway).registrar(any());

        worker.procesar(pendiente(0));

        ArgumentCaptor<CompraRegistradaRequest> contrato = ArgumentCaptor.forClass(CompraRegistradaRequest.class);
        InOrder orden = inOrder(gateway, repository);
        orden.verify(gateway).registrar(contrato.capture());
        orden.verify(repository).marcarEnviado(eq(1L), any(Instant.class));
        assertEquals(42L, contrato.getValue().idUsuario());
        assertEquals(101L, contrato.getValue().idEntrada());
        assertEquals(20L, contrato.getValue().idEvento());
        assertEquals("SHOW", contrato.getValue().tipoEvento());
        assertEquals("Tandil", contrato.getValue().ciudad());
        assertEquals(7L, contrato.getValue().artistas().get(0).idArtista());
        assertEquals("Banda", contrato.getValue().artistas().get(0).nombre());
        verify(repository, never()).guardarResultado(any(Long.class), any(String.class),
                any(Integer.class), any(Instant.class), any(String.class), any(Instant.class));
    }

    @Test
    void primerFalloRealConsumeUnIntentoYEsperaDiezSegundos() {
        prepararEnriquecimiento();
        doThrow(new RuntimeException("boom")).when(gateway).registrar(any());
        Instant antes = Instant.now().minusSeconds(1);

        worker.procesar(pendiente(0));

        Instant despues = Instant.now().plusSeconds(1);
        ArgumentCaptor<Instant> proximo = ArgumentCaptor.forClass(Instant.class);
        verify(repository).guardarResultado(eq(1L), eq(PendientePreferencia.PENDIENTE), eq(1),
                proximo.capture(), eq("boom"), any(Instant.class));
        assertTrue(!proximo.getValue().isBefore(antes.plusSeconds(10)));
        assertTrue(!proximo.getValue().isAfter(despues.plusSeconds(10)));
    }

    @Test
    void quintoFalloRealMarcaFALLIDOSinProximoIntento() {
        prepararEnriquecimiento();
        doThrow(new RuntimeException("boom")).when(gateway).registrar(any());

        worker.procesar(pendiente(4));

        verify(repository).guardarResultado(eq(1L), eq(PendientePreferencia.FALLIDO), eq(5),
                isNullInstant(), eq("boom"), any(Instant.class));
    }

    @Test
    void circuitoAbiertoNoConsumeIntentoYEsperaWaitMasCincoSegundos() {
        prepararEnriquecimiento();
        configurarEsperaDelCircuito(Duration.ofSeconds(30));
        CircuitBreaker breakerAbierto = circuitBreakerRegistry
                .circuitBreaker(ResilientPreferenciasGateway.NOMBRE_CIRCUITO);
        doThrow(CallNotPermittedException.createCallNotPermittedException(breakerAbierto))
                .when(gateway).registrar(any());
        Instant antes = Instant.now().minusSeconds(1);

        worker.procesar(pendiente(2));

        Instant despues = Instant.now().plusSeconds(1);
        ArgumentCaptor<Instant> proximo = ArgumentCaptor.forClass(Instant.class);
        verify(repository).guardarResultado(eq(1L), eq(PendientePreferencia.PENDIENTE), eq(2),
                proximo.capture(), eq("Circuito preferencias abierto"), any(Instant.class));
        assertTrue(!proximo.getValue().isBefore(antes.plusSeconds(35)));
        assertTrue(!proximo.getValue().isAfter(despues.plusSeconds(35)));
    }

    @Test
    void falloAlEnriquecerTambienCuentaComoIntentoReal() {
        when(eventoClient.obtenerPorId(20L))
                .thenThrow(new IllegalStateException("msvc-evento no responde"));

        worker.procesar(pendiente(0));

        verify(gateway, never()).registrar(any());
        verify(repository).guardarResultado(eq(1L), eq(PendientePreferencia.PENDIENTE), eq(1),
                any(Instant.class), eq("msvc-evento no responde"), any(Instant.class));
    }

    private static Instant isNullInstant() {
        return org.mockito.ArgumentMatchers.isNull();
    }

    private void configurarEsperaDelCircuito(Duration espera) {
        CircuitBreakerConfig config = mock(CircuitBreakerConfig.class);
        CircuitBreaker breaker = mock(CircuitBreaker.class);
        IntervalFunction intervalo = mock(IntervalFunction.class);
        when(intervalo.apply(1)).thenReturn(espera.toMillis());
        when(config.getWaitIntervalFunctionInOpenState()).thenReturn(intervalo);
        when(breaker.getCircuitBreakerConfig()).thenReturn(config);
        when(circuitBreakerRegistry.circuitBreaker(ResilientPreferenciasGateway.NOMBRE_CIRCUITO))
                .thenReturn(breaker);
    }

    private void prepararEnriquecimiento() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, Instant.now(), "PUBLICADO", "SHOW",
                List.of(new ParticipanteResponse(7L, "Banda", "Banda"))));
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioResponse(42L, "Tandil"));
    }

    private PendientePreferencia pendiente(int intentos) {
        PendientePreferencia pendiente = new PendientePreferencia();
        pendiente.setId(1L);
        pendiente.setIdEntrada(101L);
        pendiente.setIdUsuario(42L);
        pendiente.setIdEvento(20L);
        pendiente.setCorrelationId("corr-worker");
        pendiente.setIntentos(intentos);
        return pendiente;
    }
}
