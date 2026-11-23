package com.ticketya.notificaciones.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/** Copia el MDC (correlationId) del hilo que encola la tarea al hilo del pool; si no, se pierde la trazabilidad. */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable tarea) {
        Map<String, String> contexto = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> anterior = MDC.getCopyOfContextMap();
            try {
                if (contexto != null) {
                    MDC.setContextMap(contexto);
                } else {
                    MDC.clear();
                }
                tarea.run();
            } finally {
                if (anterior != null) {
                    MDC.setContextMap(anterior);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
