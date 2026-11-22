package com.ticketya.entrada.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Aviso pendiente hacia msvc-preferencias, persistido en la misma BD que el pago. */
@Entity
@Table(
        name = "pendientes_preferencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pendiente_preferencia_id_entrada",
                columnNames = {"id_entrada"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PendientePreferencia {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String ENVIADO = "ENVIADO";
    public static final String FALLIDO = "FALLIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_entrada", nullable = false)
    private Long idEntrada;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "proximo_intento_en")
    private Instant proximoIntentoEn;

    @Column(name = "ultimo_error", length = 2000)
    private String ultimoError;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    void sellarFechas() {
        Instant ahora = Instant.now();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        if (actualizadoEn == null) {
            actualizadoEn = ahora;
        }
    }
}
