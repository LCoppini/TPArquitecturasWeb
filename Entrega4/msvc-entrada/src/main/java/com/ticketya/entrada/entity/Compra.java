package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "compras",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_compra_usuario_idempotencia",
                columnNames = {"id_usuario", "clave_idempotencia"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    public static final String RESERVADA = "RESERVADA";
    public static final String PAGADA = "PAGADA";
    public static final String VENCIDA = "VENCIDA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String DEVUELTA = "DEVUELTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Long idCompra;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "id_tanda")
    private Long idTanda;

    @Column(name = "estado", nullable = false, length = 24)
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(name = "cantidad_entradas", nullable = false)
    private Integer cantidadEntradas;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "clave_idempotencia", nullable = false, length = 100)
    private String claveIdempotencia;

    @Column(name = "huella_solicitud", nullable = false, length = 64)
    private String huellaSolicitud;

    @Column(name = "clave_reintegro", length = 100)
    private String claveReintegro;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
