package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraCancelacionMotivoTest {

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
    void laCancelacionConservaElMotivoAunqueElReintegroSeApruebe() {
        CompraService servicio = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, "APRUEBA", 6
        );
        Compra compra = new Compra();
        compra.setIdCompra(1L);
        compra.setIdEvento(20L);
        compra.setIdUsuario(10L);
        compra.setEstado(Compra.PAGADA);
        compra.setTotal(new BigDecimal("25.00"));
        Entrada entrada = new Entrada();
        entrada.setId(101L);
        entrada.setIdCompra(1L);
        entrada.setIdEvento(20L);
        entrada.setIdSector(30L);
        entrada.setEstado("VALIDA");
        Pago pago = new Pago();
        pago.setIdPago(500L);
        pago.setEstado("APROBADO");

        when(compraRepository.findByIdEventoAndEstado(20L, Compra.RESERVADA)).thenReturn(List.of());
        when(compraRepository.findByIdEventoAndEstado(20L, Compra.PAGADA)).thenReturn(List.of(compra));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(entradaRepository.findByIdCompraOrderById(1L)).thenReturn(List.of(entrada));
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio.cancelarComprasPendientesEvento(20L);

        assertEquals("CANCELACION", entrada.getMotivoInvalidacion());
        assertEquals("DEVUELTA", entrada.getEstado());
        assertEquals(Compra.DEVUELTA, compra.getEstado());
        assertEquals("REEMBOLSADO", pago.getEstado());
    }

    @Test
    void elReintentoDeReintegroNoSobrescribeElMotivoDeCancelacion() {
        CompraService servicio = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, "APRUEBA", 6
        );
        Compra compra = new Compra();
        compra.setIdCompra(1L);
        compra.setIdEvento(20L);
        compra.setEstado(Compra.PAGADA);
        compra.setClaveReintegro("cancelacion-compra-1");
        Entrada entrada = new Entrada();
        entrada.setIdCompra(1L);
        entrada.setIdEvento(20L);
        entrada.setIdSector(30L);
        entrada.setEstado("EN_REINTEGRO");
        entrada.setMotivoInvalidacion("CANCELACION");
        Pago pago = new Pago();
        pago.setIdPago(500L);
        pago.setEstado("REINTEGRO_PENDIENTE");

        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compra));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(entradaRepository.findByIdCompraOrderById(1L)).thenReturn(List.of(entrada));
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio.reintentarDevolucion(1L);

        assertEquals("CANCELACION", entrada.getMotivoInvalidacion());
        assertEquals("REEMBOLSADO", pago.getEstado());
        assertEquals("cancelacion-compra-1", compra.getClaveReintegro());
    }
}
