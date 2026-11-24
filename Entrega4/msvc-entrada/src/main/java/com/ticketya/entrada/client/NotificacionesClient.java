package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.NotificacionRequest;
import com.ticketya.entrada.dto.request.SolicitudNotificacionRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce los hechos de entrada (compra confirmada, evento cancelado, devolucion aprobada, evento modificado) al contrato de msvc-notificaciones:
 * clave de idempotencia, datos de la plantilla como texto y nombre y fecha del evento.
 * Los hechos que no generan correo (ENTRADA_EMITIDA) se ignoran.
 */
@Component
public class NotificacionesClient {

    static final String COMPRA_CONFIRMADA = "COMPRA_CONFIRMADA";
    static final String EVENTO_CANCELADO = "EVENTO_CANCELADO";
    static final String DEVOLUCION_APROBADA = "DEVOLUCION_APROBADA";
    static final String EVENTO_MODIFICADO = "EVENTO_MODIFICADO";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private static final Locale MONEDA = Locale.forLanguageTag("es-AR");

    private final NotificacionesFeign notificacionesFeign;
    private final EventoClient eventoClient;

    public NotificacionesClient(NotificacionesFeign notificacionesFeign, EventoClient eventoClient) {
        this.notificacionesFeign = notificacionesFeign;
        this.eventoClient = eventoClient;
    }

    public void enviar(NotificacionRequest notificacion) {
        if (!COMPRA_CONFIRMADA.equals(notificacion.tipo()) && !EVENTO_CANCELADO.equals(notificacion.tipo())
                && !DEVOLUCION_APROBADA.equals(notificacion.tipo()) && !EVENTO_MODIFICADO.equals(notificacion.tipo())) {
            return;
        }
        Long idEvento = ((Number) notificacion.datos().get("idEvento")).longValue();
        EventoResponse evento = eventoClient.obtenerPorId(idEvento);
        Map<String, String> datos = new HashMap<>();
        datos.put("evento", evento.nombre());
        datos.put("fechaHora", evento.fechaHora() == null ? "a confirmar" : FECHA.format(evento.fechaHora()));
        String clave;
        if (COMPRA_CONFIRMADA.equals(notificacion.tipo())) {
            datos.put("idEntrada", idsComoTexto(notificacion.datos().get("idsEntradas")));
            clave = COMPRA_CONFIRMADA + ":idCompra:" + notificacion.datos().get("idCompra");
        } else if (DEVOLUCION_APROBADA.equals(notificacion.tipo())) {
            datos.put("idEntrada", idsComoTexto(notificacion.datos().get("idsEntradas")));
            datos.put("monto", montoComoTexto(notificacion.datos().get("monto")));
            Object idCompra = notificacion.datos().get("idCompra");
            clave = idCompra != null
                    ? DEVOLUCION_APROBADA + ":idCompra:" + idCompra
                    : DEVOLUCION_APROBADA + ":idEntrada:" + datos.get("idEntrada");
        } else if (EVENTO_MODIFICADO.equals(notificacion.tipo())) {
            String cambios = String.valueOf(notificacion.datos().get("cambios"));
            datos.put("cambios", cambios);
            clave = EVENTO_MODIFICADO + ":idEvento:" + idEvento + ":idUsuario:" + notificacion.idUsuario()
                    + ":" + Integer.toHexString(cambios.hashCode());
        } else {
            clave = EVENTO_CANCELADO + ":idEvento:" + idEvento + ":idUsuario:" + notificacion.idUsuario();
        }
        notificacionesFeign.solicitar(new SolicitudNotificacionRequest(
                clave, notificacion.idUsuario(), notificacion.tipo(), null, datos));
    }

    private String montoComoTexto(Object monto) {
        if (monto instanceof BigDecimal valor) {
            return String.format(MONEDA, "$ %,.2f", valor);
        }
        return String.valueOf(monto);
    }

    private String idsComoTexto(Object ids) {
        if (ids instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        return String.valueOf(ids);
    }
}
