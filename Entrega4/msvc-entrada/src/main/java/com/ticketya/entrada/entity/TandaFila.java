package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tanda_fila")
@Getter
@Setter
@NoArgsConstructor
public class TandaFila {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "numero", nullable = false)
    private Long numero;

    @Column(name = "estado", nullable = false, length = 16)
    private String estado;

    @Column(name = "iniciada_en", nullable = false)
    private Instant iniciadaEn;

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(name = "cupo_efectivo", nullable = false)
    private Integer cupoEfectivo;

    @Column(name = "cupo_restante", nullable = false)
    private Integer cupoRestante;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
