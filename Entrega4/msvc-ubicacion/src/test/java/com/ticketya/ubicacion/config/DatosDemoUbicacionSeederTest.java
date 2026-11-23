package com.ticketya.ubicacion.config;

import com.ticketya.ubicacion.entity.Lugar;
import com.ticketya.ubicacion.entity.Sector;
import com.ticketya.ubicacion.entity.TipoSector;
import com.ticketya.ubicacion.repository.LugarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosDemoUbicacionSeederTest {

    @Mock
    private LugarRepository lugarRepository;

    @Test
    void cargaCuatroLugaresConLosSectoresQueEsperaElSeedDeEventos() {
        when(lugarRepository.count()).thenReturn(0L);

        new DatosDemoUbicacionSeeder(lugarRepository).run(null);

        ArgumentCaptor<Lugar> guardados = ArgumentCaptor.forClass(Lugar.class);
        verify(lugarRepository, times(4)).saveAndFlush(guardados.capture());
        List<Integer> sectoresPorLugar = guardados.getAllValues().stream().map(l -> l.getSectores().size()).toList();
        assertEquals(List.of(3, 3, 2, 2), sectoresPorLugar);
    }

    @Test
    void laCapacidadDeLosSectoresNumeradosEsFilasPorAsientos() {
        when(lugarRepository.count()).thenReturn(0L);

        new DatosDemoUbicacionSeeder(lugarRepository).run(null);

        ArgumentCaptor<Lugar> guardados = ArgumentCaptor.forClass(Lugar.class);
        verify(lugarRepository, times(4)).saveAndFlush(guardados.capture());
        List<Sector> numerados = guardados.getAllValues().stream()
                .flatMap(l -> l.getSectores().stream())
                .filter(s -> s.getTipo() == TipoSector.NUMERADO)
                .toList();
        assertTrue(!numerados.isEmpty());
        numerados.forEach(s -> assertEquals(s.getFilas() * s.getAsientosPorFila(), s.getCapacidadMax().intValue()));
    }

    @Test
    void noHaceNadaSiYaHayLugares() {
        when(lugarRepository.count()).thenReturn(3L);

        new DatosDemoUbicacionSeeder(lugarRepository).run(null);

        verify(lugarRepository, never()).saveAndFlush(any());
    }
}
