package com.ticketya.evento.mapper;

import com.ticketya.evento.dto.response.EventoResponse;
import com.ticketya.evento.dto.response.EventoSectorResponse;
import com.ticketya.evento.dto.response.ParticipanteResponse;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.Participante;
import org.springframework.stereotype.Component;

import java.util.Comparator;

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

    public ParticipanteResponse aResponse(Participante participante) {
        return new ParticipanteResponse(participante.getId(), participante.getNombre(), participante.getTipo());
    }
}

