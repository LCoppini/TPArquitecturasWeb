package com.ticketya.entrada.service;

import com.ticketya.entrada.event.EventoModificadoEvent;
import com.ticketya.entrada.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModificacionEventoServiceTest {

    @Mock
    private EntradaRepository entradaRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void publicaElAvisoConLosUsuariosConEntradasVigentes() {
        when(entradaRepository.buscarUsuariosConEntradasValidas(20L)).thenReturn(List.of(10L, 11L));

        int avisados = new ModificacionEventoService(entradaRepository, eventPublisher)
                .notificarModificacion(20L, "fecha: nueva");

        ArgumentCaptor<EventoModificadoEvent> captor = ArgumentCaptor.forClass(EventoModificadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(2, avisados);
        assertEquals(20L, captor.getValue().idEvento());
        assertEquals(List.of(10L, 11L), captor.getValue().idsUsuarios());
        assertEquals("fecha: nueva", captor.getValue().cambios());
    }

    @Test
    void sinCompradoresNoPublicaNada() {
        when(entradaRepository.buscarUsuariosConEntradasValidas(20L)).thenReturn(List.of());

        int avisados = new ModificacionEventoService(entradaRepository, eventPublisher)
                .notificarModificacion(20L, "fecha: nueva");

        assertEquals(0, avisados);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
