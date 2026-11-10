package com.ticketya.evento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "evento_sector")
@Getter
@Setter
@NoArgsConstructor
public class EventoSector {

    @EmbeddedId
    private EventoSectorId id = new EventoSectorId();

    @ManyToOne(optional = false)
    @MapsId("idEvento")
    @JoinColumn(name = "id_evento", nullable = false)
    private com.ticketya.evento.entity.Evento evento;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    public EventoSector(com.ticketya.evento.entity.Evento evento, Long idSector, BigDecimal precio) {
        this.evento = evento;
        this.id.setIdSector(idSector);
        this.precio = precio;
    }

    public Long getIdSector() {
        return id.getIdSector();
    }
}
