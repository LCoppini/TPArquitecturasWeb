package com.ticketya.entrada.exception;

/** Excepción de dominio para un intento de ingreso rechazado. */
public class QrRechazadoException extends RuntimeException {

    private final QrError error;

    public QrRechazadoException(QrError error) {
        super(error.getMensaje());
        this.error = error;
    }

    public QrError getError() {
        return error;
    }
}
