package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.dto.request.EventoPublicadoRequest;
import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.EventoPublicadoResponse;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.service.AvisoEventoPublicadoService;
import com.ticketya.notificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class AvisoEventoPublicadoServiceImpl implements AvisoEventoPublicadoService {

    private final PreferenciasClient preferenciasClient;
    private final NotificacionService notificacionService;

    public AvisoEventoPublicadoServiceImpl(PreferenciasClient preferenciasClient,
                                           NotificacionService notificacionService) {
        this.preferenciasClient = preferenciasClient;
        this.notificacionService = notificacionService;
    }

    @Override
    public EventoPublicadoResponse avisar(EventoPublicadoRequest request) {
        // Primer artista (en el orden recibido) que justifica el aviso de cada usuario.
        Map<Long, String> artistaPorUsuario = new LinkedHashMap<>();
        for (EventoPublicadoRequest.ArtistaEvento artista : request.artistas()) {
            for (Long idUsuario : preferenciasClient.clientesDelArtista(artista.idArtista(), request.ciudad())
                    .idsUsuarios()) {
                artistaPorUsuario.putIfAbsent(idUsuario, artista.nombre());
            }
        }

        artistaPorUsuario.forEach((idUsuario, artista) -> solicitar(request, idUsuario, artista));
        log.info("Evento {} publicado: {} usuarios con aviso de artista favorito", request.idEvento(),
                artistaPorUsuario.size());
        return new EventoPublicadoResponse(request.idEvento(), artistaPorUsuario.size());
    }

    private void solicitar(EventoPublicadoRequest request, Long idUsuario, String artista) {
        notificacionService.solicitar(new SolicitudNotificacionRequest(
                TipoNotificacion.ARTISTA_FAVORITO_PUBLICA_EVENTO + ":idEvento:" + request.idEvento()
                        + ":idUsuario:" + idUsuario,
                idUsuario,
                TipoNotificacion.ARTISTA_FAVORITO_PUBLICA_EVENTO,
                null,
                Map.of("artista", artista, "evento", request.evento(),
                        "fechaHora", request.fechaHora(), "ciudad", request.ciudad())));
    }
}
