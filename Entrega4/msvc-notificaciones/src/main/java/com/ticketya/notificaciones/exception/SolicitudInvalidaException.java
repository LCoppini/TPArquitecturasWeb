package com.ticketya.notificaciones.exception;

/** La solicitud es sintacticamente valida pero no cumple el contrato del tipo (ej. faltan datos de la plantilla). */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
