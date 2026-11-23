package com.ticketya.entrada.service;

import com.ticketya.entrada.event.EventoCanceladoEvent;
import com.ticketya.entrada.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelacionEventoNotificacionTest {

    private final EntradaService entradaService = mock(EntradaService.class);
    private final PagoService pagoService = mock(PagoService.class);
    private final FilaVirtualService filaVirtualService = mock(FilaVirtualService.class);
    private final EntradaRepository entradaRepository = mock(EntradaRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    @Test
    void alCancelarPublicaElEventoConLosUsuariosConEntradasValidas() {
        when(entradaRepository.buscarUsuariosConEntradasValidas(20L)).thenReturn(List.of(10L, 11L));
        CancelacionEventoService service = new CancelacionEventoService(entradaService, pagoService, filaVirtualService);
        service.setNotificacion(entradaRepository, publisher);

        service.cancelarEvento(20L);

        ArgumentCaptor<EventoCanceladoEvent> captor = ArgumentCaptor.forClass(EventoCanceladoEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertEquals(List.of(10L, 11L), captor.getValue().idsUsuarios());
        verify(entradaService).cancelarEvento(20L);
    }

    @Test
    void sinUsuariosAfectadosNoPublicaNada() {
        when(entradaRepository.buscarUsuariosConEntradasValidas(20L)).thenReturn(List.of());
        CancelacionEventoService service = new CancelacionEventoService(entradaService, pagoService, filaVirtualService);
        service.setNotificacion(entradaRepository, publisher);

        service.cancelarEvento(20L);

        verify(publisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void sinConfiguracionOpcionalLaCancelacionFuncionaIgual() {
        new CancelacionEventoService(entradaService, pagoService, filaVirtualService).cancelarEvento(20L);

        verify(pagoService).cancelarEvento(20L);
    }
}
