package org.example.entrega3.Exception;

public class EstudianteException extends NotFoundException {
    public EstudianteException(String message) {
        super(message);
    }
    public EstudianteException(String message, Throwable cause) {
        super(message, cause);
    }
}
