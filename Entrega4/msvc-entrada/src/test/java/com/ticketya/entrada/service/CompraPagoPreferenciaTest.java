package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraPagoPreferenciaTest {

    @Mock private CompraRepository compraRepository;
    @Mock private EntradaRepository entradaRepository;
    @Mock private FilaEventoRepository filaEventoRepository;
    @Mock private IngresoFilaRepository ingresoFilaRepository;
    @Mock private IntentoCompraRepository intentoCompraRepository;
    @Mock private TandaFilaRepository tandaFilaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private RegistroPagoRepository registroPagoRepository;
    @Mock private PendientePreferenciaRepository pendienteRepository;
    @Mock private EventoClient eventoClient;
    @Mock private SectorClient sectorClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void ponerCorrelationId() {
        MDC.put(CorrelationContext.MDC_KEY, "corr-compra-grupal");
    }

    @AfterEach
    void limpiarCorrelationId() {
        MDC.clear();
    }

    @Test
    void pagoAprobadoRegistraUnPendientePorEntradaParaElComprador() {
        List<Long> idsEntradas = List.of(101L, 102L);
        prepararCompra(idsEntradas, "APRUEBA");
        when(pendienteRepository.findByIdEntrada(101L)).thenReturn(Optional.empty());
        when(pendienteRepository.findByIdEntrada(102L)).thenReturn(Optional.empty());

        servicio("APRUEBA").pagar(1L, 10L, "pago-grupal", new PagoCompraRequest("TARJETA"));

        ArgumentCaptor<PendientePreferencia> captor = ArgumentCaptor.forClass(PendientePreferencia.class);
        verify(pendienteRepository, times(2)).save(captor.capture());
        List<PendientePreferencia> pendientes = captor.getAllValues();
        assertEquals(Set.of(101L, 102L), pendientes.stream()
                .map(PendientePreferencia::getIdEntrada).collect(Collectors.toSet()));
        pendientes.forEach(pendiente -> {
            assertEquals(10L, pendiente.getIdUsuario());
            assertEquals(20L, pendiente.getIdEvento());
            assertEquals("corr-compra-grupal", pendiente.getCorrelationId());
            assertEquals(PendientePreferencia.PENDIENTE, pendiente.getEstado());
            assertEquals(0, pendiente.getIntentos());
        });
    }

    @Test
    void pagoRechazadoNoRegistraPendientes() {
        prepararCompra(List.of(101L), "RECHAZA");

        PagoCompraResponse respuesta = servicio("RECHAZA").pagar(1L, 10L, "pago-rechazado", new PagoCompraRequest("TARJETA"));

        assertEquals(Compra.RECHAZADA, respuesta.estado());
        verify(pendienteRepository, never()).save(any());
        verify(pendienteRepository, never()).findByIdEntrada(any());
    }

    @Test
    void pendienteYaRegistradoNoSeDuplica() {
        prepararCompra(List.of(101L, 102L), "APRUEBA");
        when(pendienteRepository.findByIdEntrada(101L))
                .thenReturn(Optional.of(new PendientePreferencia()));
        when(pendienteRepository.findByIdEntrada(102L)).thenReturn(Optional.empty());

        servicio("APRUEBA").pagar(1L, 10L, "pago-duplicado", new PagoCompraRequest("TARJETA"));

        ArgumentCaptor<PendientePreferencia> captor = ArgumentCaptor.forClass(PendientePreferencia.class);
        verify(pendienteRepository).save(captor.capture());
        assertEquals(102L, captor.getValue().getIdEntrada());
        assertTrue(captor.getValue().getCorrelationId().equals("corr-compra-grupal"));
    }

    private void prepararCompra(List<Long> idsEntradas, String resultadoPasarela) {
        Compra compra = new Compra();
        compra.setIdCompra(1L);
        compra.setIdEvento(20L);
        compra.setIdUsuario(10L);
        compra.setEstado(Compra.RESERVADA);
        compra.setVenceEn(Instant.now().plusSeconds(600));
        compra.setCantidadEntradas(idsEntradas.size());
        compra.setTotal(new BigDecimal("50.00"));

        List<Entrada> entradas = idsEntradas.stream().map(id -> {
            Entrada entrada = new Entrada();
            entrada.setId(id);
            entrada.setIdCompra(1L);
            entrada.setIdEvento(20L);
            entrada.setIdSector(30L);
            entrada.setEstado("RESERVADA");
            entrada.setPrecio(new BigDecimal("25.00"));
            return entrada;
        }).toList();

        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");

        when(compraRepository.findByIdCompraAndIdUsuario(1L, 10L)).thenReturn(Optional.of(compra));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(pagoRepository.findByIdCompraAndClaveIdempotencia(eq(1L), any(String.class)))
                .thenReturn(Optional.empty());
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            pago.setIdPago(500L);
            return pago;
        });
        when(entradaRepository.findByIdCompraOrderById(1L)).thenReturn(entradas);
        if ("RECHAZA".equals(resultadoPasarela)) {
            when(compraRepository.rechazarSiVigente(eq(1L), any(Instant.class))).thenReturn(1);
        } else {
            when(compraRepository.confirmarSiVigente(eq(1L), any(Instant.class))).thenReturn(1);
        }
    }

    private CompraService servicio(String resultadoPasarela) {
        CompraService servicio = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, resultadoPasarela, 6
        );
        servicio.setPendientePreferenciaRepository(pendienteRepository);
        return servicio;
    }
}
