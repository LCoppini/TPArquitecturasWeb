package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.event.CompraPagadaEvent;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraEventoPublicacionTest {

    @Mock private CompraRepository compraRepository;
    @Mock private EntradaRepository entradaRepository;
    @Mock private FilaEventoRepository filaEventoRepository;
    @Mock private IngresoFilaRepository ingresoFilaRepository;
    @Mock private IntentoCompraRepository intentoCompraRepository;
    @Mock private TandaFilaRepository tandaFilaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private RegistroPagoRepository registroPagoRepository;
    @Mock private EventoClient eventoClient;
    @Mock private SectorClient sectorClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CompraService compraService;

    @BeforeEach
    void iniciar() {
        compraService = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, "APRUEBA", 6
        );
        MDC.put("correlationId", "corr-pago-aprobado");
    }

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void pagoAprobadoPublicaUnUnicoCompraPagadaEventConTodasLasEntradas() {
        List<Long> idsEntradas = List.of(101L, 102L, 103L, 104L, 105L, 106L);
        Compra compra = compra(1L, idsEntradas);
        when(compraRepository.findByIdCompraAndIdUsuario(1L, 10L)).thenReturn(Optional.of(compra));
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(pagoRepository.findByIdCompraAndClaveIdempotencia(1L, "pago-aprobado"))
                .thenReturn(Optional.empty());
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            pago.setIdPago(500L);
            return pago;
        });
        when(entradaRepository.findByIdCompraOrderById(1L))
                .thenReturn(idsEntradas.stream().map(this::entrada).toList());
        when(compraRepository.confirmarSiVigente(eq(1L), any(Instant.class))).thenReturn(1);
        when(ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(
                20L, 10L, "HABILITADO")).thenReturn(Optional.empty());

        compraService.pagar(1L, 10L, "pago-aprobado", new PagoCompraRequest("TARJETA"));

        ArgumentCaptor<CompraPagadaEvent> captor = ArgumentCaptor.forClass(CompraPagadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CompraPagadaEvent evento = captor.getValue();
        assertEquals(1L, evento.idCompra());
        assertEquals(20L, evento.idEvento());
        assertEquals(10L, evento.idUsuario());
        assertEquals("corr-pago-aprobado", evento.correlationId());
        assertEquals(idsEntradas, evento.idsEntradas());
    }

    private Compra compra(Long id, List<Long> idsEntradas) {
        Compra compra = new Compra();
        compra.setIdCompra(id);
        compra.setIdEvento(20L);
        compra.setIdUsuario(10L);
        compra.setEstado(Compra.RESERVADA);
        compra.setVenceEn(Instant.now().plusSeconds(600));
        compra.setCreadaEn(Instant.now().minusSeconds(60));
        compra.setCantidadEntradas(idsEntradas.size());
        compra.setTotal(new BigDecimal("50.00"));
        return compra;
    }

    private Entrada entrada(Long id) {
        Entrada entrada = new Entrada();
        entrada.setId(id);
        entrada.setIdCompra(1L);
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setIdSector(30L);
        entrada.setEstado("RESERVADA");
        entrada.setPrecio(new BigDecimal("25.00"));
        return entrada;
    }
}
