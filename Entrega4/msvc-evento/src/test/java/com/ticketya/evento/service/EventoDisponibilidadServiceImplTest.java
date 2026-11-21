package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.DisponibilidadResponse;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.EventoSectorRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.impl.EventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoDisponibilidadServiceImplTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private EventoSectorRepository eventoSectorRepository;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private LugarClient lugarClient;
    @Mock
    private SectorClient sectorClient;
    @Mock
    private EntradaClient entradaClient;

    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(
                eventoRepository,
                eventoSectorRepository,
                participanteRepository,
                lugarClient,
                sectorClient,
                entradaClient,
                new EventoMapper()
        );
    }

    @Test
    void disponibilidadPublicaDelegaEnElContratoInterno() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(new Evento()));
        when(entradaClient.consultarDisponibilidad(1L, 2L))
                .thenReturn(new DisponibilidadResponse(1L, 2L, 100, 40L, 60L));

        DisponibilidadResponse respuesta = service.disponibilidad(1L, 2L);

        assertEquals(1L, respuesta.idEvento());
        assertEquals(2L, respuesta.idSector());
        assertEquals(60, respuesta.disponibles());
        verify(entradaClient).consultarDisponibilidad(1L, 2L);
    }

    @Test
    void disponibilidadRechazaEventoInexistenteSinConsultarEntrada() {
        when(eventoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.disponibilidad(404L, 2L));

        verify(entradaClient, never()).consultarDisponibilidad(404L, 2L);
    }
}
