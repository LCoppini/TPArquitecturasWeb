package com.ticketya.evento.service;

import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.impl.ParticipanteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipanteServiceImplTest {

    @Mock
    private ParticipanteRepository participanteRepository;

    private ParticipanteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParticipanteServiceImpl(participanteRepository, new EventoMapper());
    }

    @Test
    void obtenerPorIdDevuelveParticipanteConTipoLibre() {
        Participante participante = new Participante("Invitado", "Artista invitado");
        participante.setId(7L);
        when(participanteRepository.findById(7L)).thenReturn(Optional.of(participante));

        ParticipanteResponse respuesta = service.obtenerPorId(7L);

        assertEquals(7L, respuesta.id());
        assertEquals("Invitado", respuesta.nombre());
        assertEquals("Artista invitado", respuesta.tipo());
    }

    @Test
    void obtenerPorIdInexistenteDevuelveNoEncontrado() {
        when(participanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void listarDevuelveParticipantesMapeados() {
        Participante participante = new Participante("Banda", "Grupo");
        participante.setId(8L);
        when(participanteRepository.findAll()).thenReturn(List.of(participante));

        List<ParticipanteResponse> respuesta = service.listar();

        assertEquals(1, respuesta.size());
        assertEquals("Grupo", respuesta.getFirst().tipo());
        verify(participanteRepository).findAll();
    }
}
