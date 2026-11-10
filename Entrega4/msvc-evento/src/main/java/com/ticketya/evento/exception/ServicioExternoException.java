package com.ticketya.evento.exception;

public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
