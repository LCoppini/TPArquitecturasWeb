package com.ticketya.entrada.service;

import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.event.DevolucionAprobadaEvent;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Aviso de devolucion en el flujo de entrada individual (sin Compra). */
@ExtendWith(MockitoExtension.class)
class PagoServiceDevolucionAvisoTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private EntradaRepository entradaRepository;
    @Mock private FilaEventoRepository filaEventoRepository;
    @Mock private RegistroPagoRepository registroPagoRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void alReintegrarUnaEntradaIndividualPublicaElAviso() {
        Entrada entrada = entrada();
        Pago pago = pago("APROBADO");
        when(entradaRepository.findById(101L)).thenReturn(Optional.of(entrada));
        when(pagoRepository.findFirstByIdEntradaOrderByIdPagoDesc(101L)).thenReturn(Optional.of(pago));

        servicio("APRUEBA").reintegrarEntrada(101L);

        ArgumentCaptor<DevolucionAprobadaEvent> captor = ArgumentCaptor.forClass(DevolucionAprobadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertNull(captor.getValue().idCompra());
        assertEquals(List.of(101L), captor.getValue().idsEntradas());
        assertEquals(new BigDecimal("500.00"), captor.getValue().monto());
        assertEquals("DEVUELTA", entrada.getEstado());
    }

    @Test
    void siElReintegroFallaNoAvisa() {
        when(entradaRepository.findById(101L)).thenReturn(Optional.of(entrada()));
        when(pagoRepository.findFirstByIdEntradaOrderByIdPagoDesc(101L)).thenReturn(Optional.of(pago("APROBADO")));

        servicio("FALLA").reintegrarEntrada(101L);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private PagoService servicio(String resultado) {
        PagoService servicio = new PagoService(
                pagoRepository, entradaRepository, filaEventoRepository, registroPagoRepository, resultado);
        servicio.setEventPublisher(eventPublisher);
        return servicio;
    }

    private Entrada entrada() {
        Entrada entrada = new Entrada();
        entrada.setId(101L);
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setPrecio(new BigDecimal("500.00"));
        entrada.setEstado("EN_REINTEGRO");
        entrada.setMotivoInvalidacion("DEVOLUCION");
        return entrada;
    }

    private Pago pago(String estado) {
        Pago pago = new Pago();
        pago.setIdPago(500L);
        pago.setEstado(estado);
        return pago;
    }
}
