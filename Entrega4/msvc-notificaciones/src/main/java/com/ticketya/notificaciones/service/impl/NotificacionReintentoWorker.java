package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.service.NotificacionEnvioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Red de seguridad del envio asincrono: levanta las notificaciones que quedaron sin enviar (reintentos vencidos,
 * pool saturado, instancia caida). Es un bean aparte para que enviarAsync pase por el proxy de @Async.
 */
@Component
@Slf4j
public class NotificacionReintentoWorker {

    private final NotificacionEnvioService envioService;

    public NotificacionReintentoWorker(NotificacionEnvioService envioService) {
        this.envioService = envioService;
    }

    @Scheduled(fixedDelayString = "${ticketya.notificaciones.envio.poll-ms:5000}")
    public void procesarElegibles() {
        for (Long id : envioService.buscarElegibles()) {
            try {
                envioService.enviarAsync(id);
            } catch (TaskRejectedException e) {
                log.warn("Pool de envios saturado: el resto se retoma en la proxima pasada");
                return;
            }
        }
    }
}
