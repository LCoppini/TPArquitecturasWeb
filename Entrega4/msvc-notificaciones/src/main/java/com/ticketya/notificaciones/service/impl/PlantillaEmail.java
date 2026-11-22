package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.entity.TipoNotificacion;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantillas de texto plano por tipo. Los marcadores {clave} se reemplazan con los datos de la solicitud;
 * "nombre" es opcional y personaliza el saludo.
 */
@Component
public class PlantillaEmail {

    /** Asunto y cuerpo ya armados. */
    public record EmailRenderizado(String asunto, String cuerpo) {
    }

    private record Plantilla(List<String> requeridos, String asunto, String cuerpo) {
    }

    private static final Pattern MARCADOR = Pattern.compile("\\{(\\w+)}");

    private static final Map<TipoNotificacion, Plantilla> PLANTILLAS = new EnumMap<>(TipoNotificacion.class);

    static {
        PLANTILLAS.put(TipoNotificacion.COMPRA_CONFIRMADA, new Plantilla(
                List.of("evento", "fechaHora", "idEntrada"),
                "Tu compra para {evento} está confirmada",
                """
                {saludo}

                Tu compra fue confirmada.

                Evento: {evento}
                Fecha y hora: {fechaHora}
                Entrada Nº: {idEntrada}

                Podés ver tu entrada con su código QR en Mis entradas.
                Gracias por elegir TicketYa.
                """));
        PLANTILLAS.put(TipoNotificacion.DEVOLUCION_APROBADA, new Plantilla(
                List.of("evento", "idEntrada", "monto"),
                "Tu devolución para {evento} fue aprobada",
                """
                {saludo}

                Aprobamos la devolución de tu entrada.

                Evento: {evento}
                Entrada Nº: {idEntrada}
                Monto a reintegrar: {monto}

                Gracias por elegir TicketYa.
                """));
        PLANTILLAS.put(TipoNotificacion.EVENTO_CANCELADO, new Plantilla(
                List.of("evento", "fechaHora"),
                "El evento {evento} fue cancelado",
                """
                {saludo}

                Lamentamos informarte que el evento {evento}, previsto para el {fechaHora}, fue cancelado.
                Si compraste entradas para este evento, se gestionará el reintegro.

                TicketYa
                """));
        PLANTILLAS.put(TipoNotificacion.EVENTO_MODIFICADO, new Plantilla(
                List.of("evento", "cambios"),
                "Hubo cambios en {evento}",
                """
                {saludo}

                El evento {evento} tuvo cambios: {cambios}

                TicketYa
                """));
    }

    /** Datos obligatorios del tipo que no vienen (o vienen vacios). */
    public List<String> faltantes(TipoNotificacion tipo, Map<String, String> datos) {
        return PLANTILLAS.get(tipo).requeridos().stream()
                .filter(clave -> datos == null || datos.get(clave) == null || datos.get(clave).isBlank())
                .toList();
    }

    public EmailRenderizado renderizar(TipoNotificacion tipo, Map<String, String> datos) {
        Plantilla plantilla = PLANTILLAS.get(tipo);
        Map<String, String> valores = datos == null ? Map.of() : datos;
        String saludo = valores.get("nombre") == null || valores.get("nombre").isBlank()
                ? "Hola,"
                : "Hola " + valores.get("nombre").trim() + ",";
        String cuerpo = completar(plantilla.cuerpo().replace("{saludo}", saludo), valores);
        return new EmailRenderizado(completar(plantilla.asunto(), valores), cuerpo);
    }

    private String completar(String texto, Map<String, String> valores) {
        return MARCADOR.matcher(texto).replaceAll(resultado -> {
            String valor = valores.get(resultado.group(1));
            return Matcher.quoteReplacement(valor == null || valor.isBlank() ? "-" : valor.trim());
        });
    }
}
