package com.ticketya.notificaciones.service;

/** Envia un email de texto plano. Falla con EnvioEmailException o CircuitoAbiertoException. */
public interface EnviadorEmail {

    void enviar(String destinatario, String asunto, String cuerpo);
}
