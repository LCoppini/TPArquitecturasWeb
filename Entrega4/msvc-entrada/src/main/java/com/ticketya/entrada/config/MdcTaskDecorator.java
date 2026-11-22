package com.ticketya.entrada.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/** Copia el MDC del hilo que encola la tarea al hilo asíncrono y lo limpia al terminar. */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contexto = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contexto == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(contexto);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
