package com.ticketya.entrada.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Interruptor operativo de la fila virtual. Activado por defecto para conservar el comportamiento actual. */
@Component
@ConfigurationProperties(prefix = "ticketya.fila-virtual")
public class FilaVirtualProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
