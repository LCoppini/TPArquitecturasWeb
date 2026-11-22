package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.service.impl.NotificacionReintentoWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionReintentoWorkerTest {

    @Mock
    private NotificacionEnvioService envioService;

    @Test
    void enviaCadaNotificacionElegible() {
        when(envioService.buscarElegibles()).thenReturn(List.of(1L, 2L, 3L));

        new NotificacionReintentoWorker(envioService).procesarElegibles();

        verify(envioService).enviarAsync(1L);
        verify(envioService).enviarAsync(2L);
        verify(envioService).enviarAsync(3L);
    }

    @Test
    void siElPoolSeSaturaCortaLaPasadaYRetomaLuego() {
        when(envioService.buscarElegibles()).thenReturn(List.of(1L, 2L, 3L));
        doNothing().when(envioService).enviarAsync(1L);
        doThrow(new TaskRejectedException("pool lleno")).when(envioService).enviarAsync(2L);

        new NotificacionReintentoWorker(envioService).procesarElegibles();

        verify(envioService).enviarAsync(1L);
        verify(envioService, never()).enviarAsync(3L);
    }
}
