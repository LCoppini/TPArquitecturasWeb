package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraPropiedadTest {

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
    void configurar() {
        compraService = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, "APRUEBA", 6
        );
        when(compraRepository.findByIdCompraAndIdUsuario(500L, 11L)).thenReturn(Optional.empty());
    }

    @Test
    void consultarCompraAjenaResponde404SinRevelarLaCompra() {
        assertThrows(EntityNotFoundException.class, () -> compraService.consultar(500L, 11L));

        verify(entradaRepository, never()).findByIdCompraOrderById(500L);
    }

    @Test
    void pagarCompraAjenaResponde404SinCargoNiEvento() {
        assertThrows(EntityNotFoundException.class,
                () -> compraService.pagar(500L, 11L, "pago-ajeno", new PagoCompraRequest("TARJETA")));

        verify(pagoRepository, never()).save(any(Pago.class));
        verify(eventPublisher, never()).publishEvent(any());
        verify(filaEventoRepository, never()).bloquearEvento(any());
    }

    @Test
    void devolverCompraAjenaResponde404SinMutaciones() {
        assertThrows(EntityNotFoundException.class, () -> compraService.solicitarDevolucion(500L, 11L));

        verify(compraRepository, never()).buscarPorIdConBloqueo(500L);
        verify(entradaRepository, never()).findByIdCompraOrderById(500L);
        verify(filaEventoRepository, never()).bloquearEvento(any());
    }
}
