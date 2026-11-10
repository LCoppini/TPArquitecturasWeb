package com.ticketya.evento.service;

import com.ticketya.evento.dto.request.ConfigurarSectoresRequest;
import com.ticketya.evento.dto.request.EventoRequest;
import com.ticketya.evento.dto.response.EventoPaginaResponse;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.EventoSectorResponse;

import java.time.Instant;

public interface EventoService {

    EventoResponse crear(Long idSolicitante, EventoRequest request);

    EventoResponse obtenerPorId(Long id);

    EventoResponse actualizar(Long id, Long idSolicitante, EventoRequest request);

    EventoResponse configurarSectores(Long id, Long idSolicitante, ConfigurarSectoresRequest request);

    EventoSectorResponse obtenerPrecioSector(Long idEvento, Long idSector);

    EventoResponse publicar(Long id, Long idSolicitante);

    EventoResponse suspender(Long id, Long idSolicitante);

    EventoResponse cambiarEstadoAdministrativo(Long id, Long idSolicitante, String estado);

    EventoResponse cancelar(Long id, Long idSolicitante);

    EventoResponse reintentarCoordinacionCancelacion(Long id, Long idSolicitante);

    void eliminarFisicamente(Long id, Long idSolicitante);

    EventoPaginaResponse buscar(
            String tipo,
            Long idOrganizador,
            String participante,
            String ciudad,
            Instant desde,
            Instant hasta,
            String orden,
            int pagina,
            int tamano
    );
}
