package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraPagoFalloTest {

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

    @Test
    void falloDePasarelaCompensaLaCompraYPermiteUnNuevoIntento() {
        CompraService servicio = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, "FALLA", 6
        );
        Compra compra = new Compra();
        compra.setIdCompra(1L);
        compra.setIdEvento(20L);
        compra.setIdUsuario(10L);
        compra.setEstado(Compra.RESERVADA);
        compra.setVenceEn(Instant.now().plusSeconds(600));
        compra.setCantidadEntradas(1);
        compra.setTotal(new BigDecimal("25.00"));
        Entrada entrada = new Entrada();
        entrada.setId(101L);
        entrada.setIdCompra(1L);
        entrada.setIdEvento(20L);
        entrada.setIdSector(30L);
        entrada.setEstado("RESERVADA");
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");

        when(compraRepository.findByIdCompraAndIdUsuario(1L, 10L)).thenReturn(Optional.of(compra));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(pagoRepository.findByIdCompraAndClaveIdempotencia(1L, "pago-falla"))
                .thenReturn(Optional.empty());
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            pago.setIdPago(500L);
            return pago;
        });
        when(compraRepository.rechazarSiVigente(eq(1L), any(Instant.class))).thenReturn(1);
        when(entradaRepository.findByIdCompraOrderById(1L)).thenReturn(List.of(entrada));

        PagoCompraResponse respuesta = servicio.pagar(1L, 10L, "pago-falla", new PagoCompraRequest("TARJETA"));

        assertEquals(Compra.RECHAZADA, respuesta.estado());
        assertEquals(Compra.RECHAZADA, compra.getEstado());
        assertEquals("CANCELADA", entrada.getEstado());

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).saveAndFlush(captor.capture());
        assertEquals("FALLIDO", captor.getValue().getEstado());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
