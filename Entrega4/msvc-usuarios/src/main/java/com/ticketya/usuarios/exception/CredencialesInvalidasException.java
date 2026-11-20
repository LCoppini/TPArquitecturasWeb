package com.ticketya.usuarios.exception;

/** Credenciales rechazadas. El mensaje es generico para no revelar si el email existe. */
public class CredencialesInvalidasException extends RuntimeException {

    public static final String MENSAJE = "Credenciales inválidas.";

    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}
