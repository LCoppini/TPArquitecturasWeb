package com.ticketya.evento.mapper;

import com.ticketya.evento.dto.response.EventoDetalleResponse;
import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.EventoSectorResponse;
import com.ticketya.evento.dto.response.LugarDetalleResponse;
import com.ticketya.evento.dto.response.LugarResponse;
import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.dto.response.SectorDetalleResponse;
import com.ticketya.evento.dto.response.SectorResponse;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.Participante;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventoMapper {

    public EventoResponse aResponse(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getIdLugar(),
                evento.getIdOrganizador(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getTipo(),
                evento.getFechaHora(),
                evento.getEstado(),
                evento.getParticipantes().stream()
                        .map(relacion -> aResponse(relacion.getParticipante()))
                        .sorted(Comparator.comparing(ParticipanteResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                        .toList(),
                evento.getSectores().stream()
                        .map(relacion -> new EventoSectorResponse(relacion.getIdSector(), relacion.getPrecio()))
                        .sorted(Comparator.comparing(EventoSectorResponse::idSector))
                        .toList()
        );
    }

    public EventoDetalleResponse aDetalle(Evento evento, LugarResponse lugar) {
        Map<Long, SectorResponse> sectoresDelLugar = lugar.sectores() == null ? Map.of()
                : lugar.sectores().stream()
                .collect(Collectors.toMap(SectorResponse::idSector, Function.identity(), (a, b) -> a));
        List<SectorDetalleResponse> sectores = evento.getSectores().stream()
                .map(relacion -> aDetalle(relacion.getIdSector(), relacion.getPrecio(),
                        sectoresDelLugar.get(relacion.getIdSector())))
                .sorted(Comparator.comparing(SectorDetalleResponse::idSector))
                .toList();
        return new EventoDetalleResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getTipo(),
                evento.getFechaHora(),
                evento.getEstado(),
                new LugarDetalleResponse(lugar.nombre(), lugar.direccion(), lugar.ciudad()),
                evento.getParticipantes().stream()
                        .map(relacion -> aResponse(relacion.getParticipante()))
                        .sorted(Comparator.comparing(ParticipanteResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                        .toList(),
                sectores
        );
    }

    private SectorDetalleResponse aDetalle(Long idSector, java.math.BigDecimal precio, SectorResponse sector) {
        if (sector == null) {
            return new SectorDetalleResponse(idSector, null, null, null, null, null, precio);
        }
        return new SectorDetalleResponse(idSector, sector.nombre(), sector.tipo(), sector.capacidadMax(),
                sector.filas(), sector.asientosPorFila(), precio);
    }

    public ParticipanteResponse aResponse(Participante participante) {
        return new ParticipanteResponse(participante.getId(), participante.getNombre(), participante.getTipo());
    }
}

