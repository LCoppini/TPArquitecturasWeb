package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.event.DevolucionAprobadaEvent;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El aviso de devolucion se publica solo cuando el reintegro de una devolucion queda concretado. */
@ExtendWith(MockitoExtension.class)
class DevolucionAprobadaPublicacionTest {

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

    private Compra compra;
    private Entrada entrada;
    private Pago pago;

    @BeforeEach
    void setUp() {
        compra = new Compra();
        compra.setIdCompra(1L);
        compra.setIdEvento(20L);
        compra.setIdUsuario(10L);
        compra.setEstado(Compra.PAGADA);
        compra.setTotal(new BigDecimal("18000.00"));
        entrada = new Entrada();
        entrada.setId(101L);
        entrada.setIdCompra(1L);
        entrada.setIdEvento(20L);
        entrada.setIdSector(30L);
        entrada.setEstado("EN_REINTEGRO");
        entrada.setMotivoInvalidacion("DEVOLUCION");
        pago = new Pago();
        pago.setIdPago(500L);
        pago.setEstado("APROBADO");
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compra));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(compraRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(entradaRepository.findByIdCompraOrderById(1L)).thenReturn(List.of(entrada));
    }

    @Test
    void alAprobarYReintegrarPublicaElAvisoConElMontoDeLaCompra() {
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio("APRUEBA").resolverDevolucion(1L, true);

        ArgumentCaptor<DevolucionAprobadaEvent> captor = ArgumentCaptor.forClass(DevolucionAprobadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        DevolucionAprobadaEvent evento = captor.getValue();
        assertEquals(1L, evento.idCompra());
        assertEquals(20L, evento.idEvento());
        assertEquals(10L, evento.idUsuario());
        assertEquals(List.of(101L), evento.idsEntradas());
        assertEquals(new BigDecimal("18000.00"), evento.monto());
    }

    @Test
    void siElReintegroFallaNoAvisaHastaCompletarlo() {
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio("FALLA").resolverDevolucion(1L, true);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        assertEquals("REINTEGRO_PENDIENTE", pago.getEstado());
    }

    @Test
    void alReintentarYCompletarElReintegroPublicaElAviso() {
        pago.setEstado("REINTEGRO_PENDIENTE");
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio("APRUEBA").reintentarDevolucion(1L);

        verify(eventPublisher).publishEvent(any(DevolucionAprobadaEvent.class));
    }

    @Test
    void unReintegroPorCancelacionDeEventoNoAvisaDevolucion() {
        entrada.setMotivoInvalidacion("CANCELACION");
        pago.setEstado("REINTEGRO_PENDIENTE");
        when(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(1L)).thenReturn(Optional.of(pago));

        servicio("APRUEBA").reintentarDevolucion(1L);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void rechazarLaDevolucionNoAvisa() {
        when(pagoRepository.findFirstByIdCompraAndEstado(1L, "APROBADO")).thenReturn(Optional.of(pago));

        servicio("APRUEBA").resolverDevolucion(1L, false);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        assertEquals("VALIDA", entrada.getEstado());
    }

    private CompraService servicio(String resultadoPasarela) {
        return new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, resultadoPasarela, 6
        );
    }
}
