package com.ticketya.notificaciones.service;

import java.util.List;

/**
 * Envio de una notificacion ya registrada. enviarAsync debe invocarse desde OTRO bean: si se llama desde
 * esta misma clase, el proxy de @Async no interviene y el envio corre en el hilo del llamador.
 */
public interface NotificacionEnvioService {

    /** Envia en un hilo del pool propio (@Async). */
    void enviarAsync(Long id);

    /** Reclama y envia en el hilo actual. Si otro hilo o instancia la reclamo antes, no hace nada. */
    void procesar(Long id);

    /** Ids de notificaciones que corresponde (re)intentar ahora. */
    List<Long> buscarElegibles();
}
