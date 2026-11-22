package com.ticketya.notificaciones.dto.request;

import com.ticketya.notificaciones.entity.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Solicitud interna de envio, invocada por otros microservicios (msvc-entrada, msvc-evento). */
public record SolicitudNotificacionRequest(
        @Schema(description = "Identifica el hecho a notificar; repetirla no genera otro envio",
                example = "COMPRA_CONFIRMADA:idEntrada:101")
        @NotBlank @Size(max = 120) String claveIdempotencia,
        @Schema(description = "Usuario destinatario (referencia por id)", example = "42")
        @NotNull Long idUsuario,
        @NotNull TipoNotificacion tipo,
        @Schema(description = "Opcional. Si falta, se consulta el email del usuario a msvc-usuarios",
                example = "cliente@correo.com")
        @Email @Size(max = 255) String destinatario,
        @Schema(description = "Datos de la plantilla segun el tipo (evento, fechaHora, idEntrada, monto, cambios; nombre es opcional)",
                example = "{\"evento\":\"Ritual en vivo\",\"fechaHora\":\"24/10 21:00\",\"idEntrada\":\"101\"}")
        Map<String, String> datos
) {
}
