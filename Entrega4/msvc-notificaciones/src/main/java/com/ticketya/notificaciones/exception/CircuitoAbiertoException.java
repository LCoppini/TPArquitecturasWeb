package com.ticketya.notificaciones.exception;

/** El circuito del envio esta abierto: no se intento enviar, por lo que no consume un intento. */
public class CircuitoAbiertoException extends RuntimeException {

    public CircuitoAbiertoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
