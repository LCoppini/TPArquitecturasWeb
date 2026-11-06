package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "entradas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entrada_usuario_clave_idempotencia",
                columnNames = {"id_usuario", "clave_idempotencia"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_sector", nullable = false)
    private Long idSector;

    @Column(name = "fila")
    private Integer fila;

    @Column(name = "asiento")
    private Integer asiento;

    @Column(name = "estado", nullable = false, length = 24)
    private String estado;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "codigo_qr")
    private String codigoQr;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "clave_idempotencia", length = 100)
    private String claveIdempotencia;

    @Column(name = "estado_devolucion", length = 24)
    private String estadoDevolucion;

    @OneToMany(mappedBy = "entrada")
    private List<RegistroPago> registrosPago = new ArrayList<>();

}
