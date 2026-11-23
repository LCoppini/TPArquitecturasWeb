package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "intento_compra")
@Getter
@Setter
@NoArgsConstructor
public class IntentoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "id_tanda", nullable = false)
    private Long idTanda;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "resultado", nullable = false, length = 40)
    private String resultado;
}
