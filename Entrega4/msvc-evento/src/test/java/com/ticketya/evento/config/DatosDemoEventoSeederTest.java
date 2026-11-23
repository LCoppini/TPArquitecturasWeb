package com.ticketya.evento.config;

import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosDemoEventoSeederTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private ParticipanteRepository participanteRepository;

    private List<Evento> ejecutar() {
        when(eventoRepository.count()).thenReturn(0L);
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(participanteRepository.save(any(Participante.class))).thenAnswer(i -> i.getArgument(0));

        new DatosDemoEventoSeeder(eventoRepository, participanteRepository, 2L).run(null);

        ArgumentCaptor<Evento> guardados = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository, atLeastOnce()).save(guardados.capture());
        return guardados.getAllValues();
    }

    @Test
    void cargaEventosPublicadosFuturosConPrecioPorSector() {
        List<Evento> eventos = ejecutar();

        List<Evento> publicados = eventos.stream()
                .filter(e -> EstadosEvento.PUBLICADO.equals(e.getEstado()))
                .toList();
        assertTrue(publicados.size() >= 16);
        publicados.forEach(e -> {
            assertTrue(e.getFechaHora().isAfter(Instant.now()));
            assertTrue(!e.getSectores().isEmpty());
        });
    }

    @Test
    void incluyeLosEstadosNecesariosParaProbarLasPantallas() {
        List<String> estados = ejecutar().stream().map(Evento::getEstado).distinct().toList();

        assertTrue(estados.containsAll(List.of(
                EstadosEvento.PUBLICADO, EstadosEvento.AGOTADO, EstadosEvento.CANCELADO,
                EstadosEvento.BORRADOR, EstadosEvento.FINALIZADO)));
    }

    @Test
    void asignaElOrganizadorConfigurado() {
        ejecutar().forEach(e -> assertEquals(Long.valueOf(2L), e.getIdOrganizador()));
    }

    @Test
    void noHaceNadaSiYaHayEventos() {
        when(eventoRepository.count()).thenReturn(5L);

        new DatosDemoEventoSeeder(eventoRepository, participanteRepository, 2L).run(null);

        verify(eventoRepository, never()).save(any());
    }
}
