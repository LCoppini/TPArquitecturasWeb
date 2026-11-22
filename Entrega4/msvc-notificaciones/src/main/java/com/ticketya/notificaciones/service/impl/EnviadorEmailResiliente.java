package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.exception.CircuitoAbiertoException;
import com.ticketya.notificaciones.exception.EnvioEmailException;
import com.ticketya.notificaciones.service.EnviadorEmail;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Protege el envio SMTP con Circuit Breaker + TimeLimiter (instancia "email", configurada en YAML).
 * No usa retry de Resilience4j: los reintentos los lleva el historial persistido, asi cada intento es real.
 */
@Component
@Primary
public class EnviadorEmailResiliente implements EnviadorEmail {

    static final String INSTANCIA = "email";

    private final EnviadorEmailSmtp smtp;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public EnviadorEmailResiliente(EnviadorEmailSmtp smtp, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.smtp = smtp;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        CircuitBreaker circuito = circuitBreakerFactory.create(INSTANCIA);
        try {
            circuito.run(() -> {
                smtp.enviar(destinatario, asunto, cuerpo);
                return null;
            });
        } catch (RuntimeException e) {
            if (contieneCausa(e, CallNotPermittedException.class)) {
                throw new CircuitoAbiertoException("El circuito de envio de emails esta abierto", e);
            }
            if (e instanceof EnvioEmailException envio) {
                throw envio;
            }
            throw new EnvioEmailException("Fallo el envio del email: " + e.getMessage(), e);
        }
    }

    private static boolean contieneCausa(Throwable error, Class<? extends Throwable> tipo) {
        Throwable actual = error;
        int profundidad = 0;
        while (actual != null && profundidad < 10) {
            if (tipo.isInstance(actual)) {
                return true;
            }
            actual = actual.getCause();
            profundidad++;
        }
        return false;
    }
}
