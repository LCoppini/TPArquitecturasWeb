package com.ticketya.preferencias.service.impl;

import com.ticketya.preferencias.client.EventoClient;
import com.ticketya.preferencias.client.UsuarioClient;
import com.ticketya.preferencias.client.dto.EventoBusquedaResponse;
import com.ticketya.preferencias.client.dto.EventoItem;
import com.ticketya.preferencias.client.dto.UsuarioItem;
import com.ticketya.preferencias.dto.response.RecomendacionResponse;
import com.ticketya.preferencias.entity.ArtistaPreferido;
import com.ticketya.preferencias.entity.PreferenciaCliente;
import com.ticketya.preferencias.repository.PreferenciaClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecomendacionServiceImplTest {

    private PreferenciaClienteRepository repository;
    private EventoClient eventoClient;
    private UsuarioClient usuarioClient;
    private RecomendacionServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PreferenciaClienteRepository.class);
        eventoClient = mock(EventoClient.class);
        usuarioClient = mock(UsuarioClient.class);
        service = new RecomendacionServiceImpl(repository, eventoClient, usuarioClient);
    }

    @Test
    void coincidenciaPorTipoDevuelveMotivoMismoTipo() {
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil",
                Map.of("SHOW", 1), List.of(), Set.of())));
        whenBuscar("SHOW", null, null, List.of(evento(20, "SHOW", 3600)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(1, resultado.size());
        assertEquals(20L, resultado.get(0).idEvento());
        assertEquals("SHOW", resultado.get(0).tipo());
        assertEquals(RecomendacionServiceImpl.MISMO_TIPO, resultado.get(0).motivo());
    }

    @Test
    void coincidenciaPorArtistaDevuelveMotivoMismoArtista() {
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil",
                Map.of(), List.of(new ArtistaPreferido(7L, "Banda", 1)), Set.of())));
        whenBuscar(null, "7", null, List.of(evento(21, "SHOW", 3600)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(1, resultado.size());
        assertEquals(RecomendacionServiceImpl.MISMO_ARTISTA, resultado.get(0).motivo());
    }

    @Test
    void soloSeConsideranLosCincoArtistasYTresTiposConMayorCantidad() {
        Map<String, Integer> tipos = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            tipos.put("T" + i, i);
        }
        List<ArtistaPreferido> artistas = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            artistas.add(new ArtistaPreferido((long) i, "Artista " + i, i));
        }
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil", tipos, artistas, Set.of())));
        whenBuscar("T5", null, null, List.of(evento(20, "T5", 3600)));

        service.recomendar(42L);

        for (String tipo : List.of("T5", "T4", "T3")) {
            verify(eventoClient).buscar(eq(tipo), eq(null), eq(null), any(Instant.class),
                    eq("fecha"), eq(0), eq(50));
        }
        for (String tipo : List.of("T2", "T1")) {
            verify(eventoClient, never()).buscar(eq(tipo), eq(null), eq(null), any(Instant.class),
                    eq("fecha"), eq(0), eq(50));
        }
        for (long idArtista : List.of(7L, 6L, 5L, 4L, 3L)) {
            verify(eventoClient).buscar(eq(null), eq(Long.toString(idArtista)), eq(null),
                    any(Instant.class), eq("fecha"), eq(0), eq(50));
        }
        for (long idArtista : List.of(2L, 1L)) {
            verify(eventoClient, never()).buscar(eq(null), eq(Long.toString(idArtista)), eq(null),
                    any(Instant.class), eq("fecha"), eq(0), eq(50));
        }
    }

    @Test
    void desempataPorIdentificadorAscendente() {
        List<ArtistaPreferido> artistas = List.of(
                new ArtistaPreferido(9L, "Nueve", 2),
                new ArtistaPreferido(2L, "Dos", 2),
                new ArtistaPreferido(5L, "Cinco", 2));
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil", Map.of(), artistas, Set.of())));
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioItem(42L, ""));

        service.recomendar(42L);

        InOrder orden = inOrder(eventoClient);
        for (String id : List.of("2", "5", "9")) {
            orden.verify(eventoClient).buscar(eq(null), eq(id), eq(null), any(Instant.class),
                    eq("fecha"), eq(0), eq(50));
        }
    }

    @Test
    void excluyeEventosPasadosYaCompradosEnLasCoincidencias() {
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil",
                Map.of("SHOW", 1), List.of(), Set.of(20L))));
        whenBuscar("SHOW", null, null, List.of(
                evento(20, "SHOW", 3600),
                evento(21, "SHOW", -3600),
                evento(22, "SHOW", 7200)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(List.of(22L), resultado.stream().map(RecomendacionResponse::idEvento).toList());
    }

    @Test
    void fallbackPorCiudadCuandoNoHayCoincidencias() {
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil",
                Map.of("SHOW", 1), List.of(), Set.of())));
        whenBuscar("SHOW", null, null, List.of());
        whenBuscar(null, null, "Tandil", List.of(evento(30, "TEATRO", 3600)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(1, resultado.size());
        assertEquals(RecomendacionServiceImpl.FALLBACK_CIUDAD, resultado.get(0).motivo());
        ArgumentCaptor<Instant> desde = ArgumentCaptor.forClass(Instant.class);
        verify(eventoClient).buscar(eq(null), eq(null), eq("Tandil"), desde.capture(),
                eq("fecha"), eq(0), eq(50));
        assertTrue(!desde.getValue().isBefore(Instant.now().minusSeconds(5)));
    }

    @Test
    void sinDocumentoObtieneLaCiudadDelUsuario() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioItem(42L, "Buenos Aires"));
        whenBuscar(null, null, "Buenos Aires", List.of(evento(31, "SHOW", 3600)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(List.of(31L), resultado.stream().map(RecomendacionResponse::idEvento).toList());
        assertEquals(RecomendacionServiceImpl.FALLBACK_CIUDAD, resultado.get(0).motivo());
        verify(eventoClient, never()).buscar(eq("SHOW"), eq(null), eq(null), any(Instant.class),
                eq("fecha"), eq(0), eq(50));
    }

    @Test
    void sinCiudadDevuelveListaVacia() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        when(usuarioClient.obtenerPorId(42L)).thenReturn(new UsuarioItem(42L, "  "));

        assertTrue(service.recomendar(42L).isEmpty());
        verify(eventoClient, never()).buscar(any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void fallbackTambienExcluyeEventosYaComprados() {
        when(repository.findById(42L)).thenReturn(Optional.of(preferencia("Tandil",
                Map.of("SHOW", 1), List.of(), Set.of(30L))));
        whenBuscar("SHOW", null, null, List.of());
        whenBuscar(null, null, "Tandil", List.of(evento(30, "SHOW", 3600), evento(31, "SHOW", 7200)));

        List<RecomendacionResponse> resultado = service.recomendar(42L);

        assertEquals(List.of(31L), resultado.stream().map(RecomendacionResponse::idEvento).toList());
    }

    private void whenBuscar(String tipo, String participante, String ciudad, List<EventoItem> eventos) {
        when(eventoClient.buscar(eq(tipo), eq(participante), eq(ciudad), any(Instant.class),
                eq("fecha"), eq(0), eq(50))).thenReturn(new EventoBusquedaResponse(eventos));
    }

    private EventoItem evento(long id, String tipo, long segundos) {
        return new EventoItem(id, tipo, Instant.now().plusSeconds(segundos), "PUBLICADO", List.of());
    }

    private PreferenciaCliente preferencia(String ciudad, Map<String, Integer> tipos,
                                           List<ArtistaPreferido> artistas, Set<Long> excluidos) {
        PreferenciaCliente preferencia = new PreferenciaCliente(42L);
        preferencia.setCiudad(ciudad);
        preferencia.setTiposEvento(new LinkedHashMap<>(tipos));
        preferencia.setArtistas(new ArrayList<>(artistas));
        preferencia.setEventosComprados(new LinkedHashSet<>(excluidos));
        return preferencia;
    }
}
