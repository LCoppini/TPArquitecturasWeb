package org.example.entrega3.Exception;

public class CarreraException extends NotFoundException {
    public CarreraException(String message) {
        super(message);
    }
    public CarreraException(String message, Throwable cause) {
        super(message, cause);
    }
}
