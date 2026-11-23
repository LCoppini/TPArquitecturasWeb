package com.ticketya.notificaciones.exception;

/** El envio por SMTP fallo (error del servidor, timeout o conexion). Se reintenta segun el backoff. */
public class EnvioEmailException extends RuntimeException {

    public EnvioEmailException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
