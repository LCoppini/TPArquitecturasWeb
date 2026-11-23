package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fila_evento")
@Getter
@Setter
@NoArgsConstructor
public class FilaEvento {

    @Id
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "causa_cierre", length = 24)
    private String causaCierre;

    @Column(name = "max_personas", nullable = false)
    private Integer maxPersonas;

    @Column(name = "cupo_entradas", nullable = false)
    private Integer cupoEntradas;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "siguiente_secuencia", nullable = false)
    private Long siguienteSecuencia;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
