package com.ticketya.preferencias.service.impl;

import com.ticketya.preferencias.client.EventoClient;
import com.ticketya.preferencias.client.UsuarioClient;
import com.ticketya.preferencias.client.dto.EventoBusquedaResponse;
import com.ticketya.preferencias.client.dto.EventoItem;
import com.ticketya.preferencias.dto.response.RecomendacionResponse;
import com.ticketya.preferencias.entity.ArtistaPreferido;
import com.ticketya.preferencias.entity.PreferenciaCliente;
import com.ticketya.preferencias.repository.PreferenciaClienteRepository;
import com.ticketya.preferencias.service.RecomendacionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RecomendacionServiceImpl implements RecomendacionService {

    static final String MISMO_TIPO = "MISMO_TIPO";
    static final String MISMO_ARTISTA = "MISMO_ARTISTA";
    static final String FALLBACK_CIUDAD = "FALLBACK_CIUDAD";

    private static final int MAX_ARTISTAS = 5;
    private static final int MAX_TIPOS = 3;
    private static final int TAMANO_PAGINA = 50;
    private static final String ORDEN_FECHA = "fecha";

    private final PreferenciaClienteRepository repository;
    private final EventoClient eventoClient;
    private final UsuarioClient usuarioClient;

    public RecomendacionServiceImpl(PreferenciaClienteRepository repository,
                                    EventoClient eventoClient,
                                    UsuarioClient usuarioClient) {
        this.repository = repository;
        this.eventoClient = eventoClient;
        this.usuarioClient = usuarioClient;
    }

    @Override
    public List<RecomendacionResponse> recomendar(Long idUsuario) {
        Optional<PreferenciaCliente> preferencia = repository.findById(idUsuario);
        Instant ahora = Instant.now();
        Set<Long> excluidos = preferencia.map(PreferenciaCliente::getEventosComprados)
                .orElseGet(LinkedHashSet::new);
        if (excluidos == null) {
            excluidos = Set.of();
        }

        Map<Long, RecomendacionResponse> coincidencias = new LinkedHashMap<>();
        if (preferencia.isPresent()) {
            List<Long> idsArtistas = artistasTop(preferencia.get());
            List<String> tipos = tiposTop(preferencia.get());

            for (String tipo : tipos) {
                agregar(buscar(eventoClient.buscar(tipo, null, null, ahora, ORDEN_FECHA, 0, TAMANO_PAGINA),
                        ahora, excluidos, MISMO_TIPO), coincidencias);
            }
            for (Long idArtista : idsArtistas) {
                agregar(buscar(eventoClient.buscar(null, idArtista.toString(), null, ahora,
                        ORDEN_FECHA, 0, TAMANO_PAGINA), ahora, excluidos, MISMO_ARTISTA), coincidencias);
            }
        }

        if (!coincidencias.isEmpty()) {
            return new ArrayList<>(coincidencias.values());
        }

        String ciudad = ciudad(preferencia.orElse(null), idUsuario);
        if (ciudad == null || ciudad.isBlank()) {
            return List.of();
        }
        List<RecomendacionResponse> fallback = buscar(
                eventoClient.buscar(null, null, ciudad, ahora, ORDEN_FECHA, 0, TAMANO_PAGINA),
                ahora, excluidos, FALLBACK_CIUDAD);
        return new ArrayList<>(fallback);
    }

    private String ciudad(PreferenciaCliente preferencia, Long idUsuario) {
        if (preferencia != null) {
            return preferencia.getCiudad();
        }
        var usuario = usuarioClient.obtenerPorId(idUsuario);
        return usuario == null ? null : usuario.ciudad();
    }

    private List<Long> artistasTop(PreferenciaCliente preferencia) {
        return preferencia.getArtistas().stream()
                .sorted(Comparator.comparingInt(ArtistaPreferido::getCantidad).reversed()
                        .thenComparing(ArtistaPreferido::getIdArtista,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_ARTISTAS)
                .map(ArtistaPreferido::getIdArtista)
                .toList();
    }

    private List<String> tiposTop(PreferenciaCliente preferencia) {
        return preferencia.getTiposEvento().entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_TIPOS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<RecomendacionResponse> buscar(EventoBusquedaResponse respuesta, Instant ahora,
                                               Set<Long> excluidos, String motivo) {
        if (respuesta == null || respuesta.contenido() == null) {
            return List.of();
        }
        return respuesta.contenido().stream()
                .filter(evento -> evento.fechaHora() != null && !evento.fechaHora().isBefore(ahora))
                .filter(evento -> !excluidos.contains(evento.id()))
                .map(evento -> new RecomendacionResponse(evento.id(), evento.fechaHora(),
                        evento.tipo(), motivo))
                .toList();
    }

    private void agregar(List<RecomendacionResponse> candidatos,
                         Map<Long, RecomendacionResponse> destino) {
        for (RecomendacionResponse candidato : candidatos) {
            destino.putIfAbsent(candidato.idEvento(), candidato);
        }
    }
}
