package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.service.FilaVirtualService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilaVirtualSchedulerTest {

    @Test
    void errorEnUnEventoNoImpideProcesarLosSiguientes() {
        TandaFilaRepository repository = mock(TandaFilaRepository.class);
        FilaVirtualService service = mock(FilaVirtualService.class);
        when(repository.buscarEventosConTandaVencida(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(20L, 21L));
        doThrow(new IllegalStateException("falla de prueba"))
                .when(service).cerrarTandaVencida(20L);
        FilaVirtualScheduler scheduler = new FilaVirtualScheduler(repository, service);

        scheduler.cerrarTandasVencidas();

        verify(service).cerrarTandaVencida(20L);
        verify(service).cerrarTandaVencida(21L);
    }

    @Test
    void falloTransitorioSeReintentaEnElSiguienteTick() {
        TandaFilaRepository repository = mock(TandaFilaRepository.class);
        FilaVirtualService service = mock(FilaVirtualService.class);
        when(repository.buscarEventosConTandaVencida(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(20L));
        doThrow(new IllegalStateException("fallo transitorio"))
                .doNothing()
                .when(service).cerrarTandaVencida(20L);
        FilaVirtualScheduler scheduler = new FilaVirtualScheduler(repository, service);

        scheduler.cerrarTandasVencidas();
        scheduler.cerrarTandasVencidas();

        verify(service, times(2)).cerrarTandaVencida(20L);
    }
}
