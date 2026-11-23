package com.ticketya.notificaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Historial de envios. id_usuario es una referencia por id (sin FK hacia msvc-usuarios). */
@Entity
@Table(name = "notificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_clave", columnNames = "clave_idempotencia"),
        indexes = {
                @Index(name = "idx_notificacion_estado_proximo", columnList = "estado, proximo_intento_en"),
                @Index(name = "idx_notificacion_usuario", columnList = "id_usuario")
        })
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La define quien solicita (ej. COMPRA_CONFIRMADA:idEntrada:101): evita duplicar el aviso ante reintentos. */
    @Column(name = "clave_idempotencia", nullable = false, length = 120)
    private String claveIdempotencia;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoNotificacion tipo;

    /** Viene en la solicitud o se resuelve consultando a msvc-usuarios al enviar. */
    @Column(name = "destinatario", length = 255)
    private String destinatario;

    @Column(name = "asunto", length = 255)
    private String asunto;

    /** Datos que completan la plantilla (evento, fechaHora, idEntrada, ...). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos", columnDefinition = "jsonb")
    private Map<String, String> datos = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "proximo_intento_en")
    private Instant proximoIntentoEn;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    /** Se restaura en el MDC al enviar, tambien en los reintentos. */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    @PrePersist
    void establecerFechas() {
        Instant ahora = Instant.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = fechaCreacion;
        }
    }
}
