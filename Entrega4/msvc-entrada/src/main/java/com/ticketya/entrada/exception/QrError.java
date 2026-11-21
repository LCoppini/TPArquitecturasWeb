package com.ticketya.entrada.exception;

import org.springframework.http.HttpStatus;

/** Motivos de rechazo del escaneo de QR, con su código estable y estado HTTP. */
public enum QrError {

    QR_REQUEST_INVALIDO("La solicitud de escaneo es inválida", HttpStatus.BAD_REQUEST),
    QR_NO_EXISTE("El QR no existe o fue alterado", HttpStatus.NOT_FOUND),
    QR_EVENTO_DISTINTO("La entrada corresponde a otro evento", HttpStatus.CONFLICT),
    QR_DEVUELTO("La entrada fue devuelta", HttpStatus.CONFLICT),
    QR_EVENTO_CANCELADO("El evento fue cancelado", HttpStatus.CONFLICT),
    QR_VENCIDO("La reserva venció o fue cancelada", HttpStatus.CONFLICT),
    QR_YA_USADO("La entrada ya fue utilizada", HttpStatus.CONFLICT);

    private final String mensaje;
    private final HttpStatus httpStatus;

    QrError(String mensaje, HttpStatus httpStatus) {
        this.mensaje = mensaje;
        this.httpStatus = httpStatus;
    }

    public String getMensaje() {
        return mensaje;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
