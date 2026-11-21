package com.ticketya.evento.service;

import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.exception.ReglaNegocioException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.impl.ParticipanteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminParticipanteServiceImplTest {

    @Mock
    private ParticipanteRepository participanteRepository;

    private ParticipanteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParticipanteServiceImpl(participanteRepository, new EventoMapper());
    }

    @Test
    void crearReutilizaArtistaExistentePorNombreYTipo() {
        Participante existente = new Participante("Banda", "Grupo");
        existente.setId(5L);
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase("Banda", "Grupo"))
                .thenReturn(Optional.of(existente));

        ParticipanteResponse respuesta = service.crear(new ParticipanteRequest(null, " Banda ", " Grupo "));

        assertEquals(5L, respuesta.id());
        assertEquals("Banda", respuesta.nombre());
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void crearGuardaArtistaNuevoNormalizandoEspacios() {
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase("Solista", "Música"))
                .thenReturn(Optional.empty());
        when(participanteRepository.save(any(Participante.class))).thenAnswer(invocation -> {
            Participante guardado = invocation.getArgument(0);
            guardado.setId(7L);
            return guardado;
        });

        ParticipanteResponse respuesta = service.crear(new ParticipanteRequest(null, " Solista ", " Música "));

        assertEquals(7L, respuesta.id());
        ArgumentCaptor<Participante> captor = ArgumentCaptor.forClass(Participante.class);
        verify(participanteRepository).save(captor.capture());
        assertEquals("Solista", captor.getValue().getNombre());
        assertEquals("Música", captor.getValue().getTipo());
    }

    @Test
    void crearRechazaNombreVacio() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.crear(new ParticipanteRequest(null, "  ", "Grupo")));

        assertEquals("El nombre del artista es obligatorio", error.getMessage());
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void actualizarCambiaNombreYTipo() {
        Participante existente = new Participante("Banda", "Grupo");
        existente.setId(5L);
        when(participanteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase("Solista", "Música"))
                .thenReturn(Optional.empty());
        when(participanteRepository.save(existente)).thenReturn(existente);

        ParticipanteResponse respuesta = service.actualizar(5L,
                new ParticipanteRequest(5L, " Solista ", " Música "));

        assertEquals(5L, respuesta.id());
        assertEquals("Solista", respuesta.nombre());
        assertEquals("Música", respuesta.tipo());
    }

    @Test
    void actualizarRechazaDuplicadoDistinto() {
        Participante existente = new Participante("Banda", "Grupo");
        existente.setId(5L);
        Participante otro = new Participante("Solista", "Música");
        otro.setId(9L);
        when(participanteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase("Solista", "Música"))
                .thenReturn(Optional.of(otro));

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(5L, new ParticipanteRequest(5L, "Solista", "Música")));

        verify(participanteRepository, never()).save(any());
    }

    @Test
    void actualizarRechazaIdInexistente() {
        when(participanteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(404L, new ParticipanteRequest(404L, "Solista", "Música")));

        verify(participanteRepository, never()).save(any());
    }
}
