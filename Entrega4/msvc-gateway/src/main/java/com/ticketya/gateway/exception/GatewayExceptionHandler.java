package com.ticketya.gateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<GatewayErrorResponse> servicioNoDisponible(
            ResourceAccessException exception,
            HttpServletRequest request
    ) {
        // Registrar la causa para diagnóstico sin exponerla en el body público.
        LOGGER.warn("No se pudo contactar al servicio destino para {}", request.getRequestURI(), exception);
        return respuestaServicioNoDisponible(request);
    }

    @ExceptionHandler(HttpServerErrorException.ServiceUnavailable.class)
    public ResponseEntity<GatewayErrorResponse> servicioSinInstancias(
            HttpServerErrorException.ServiceUnavailable exception,
            HttpServletRequest request
    ) {
        // El filtro lb de Gateway MVC expresa la ausencia de instancias como un 503.
        LOGGER.warn("Eureka no encontró una instancia destino para {}", request.getRequestURI());
        return respuestaServicioNoDisponible(request);
    }

    private ResponseEntity<GatewayErrorResponse> respuestaServicioNoDisponible(HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        GatewayErrorResponse response = new GatewayErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "El servicio solicitado no está disponible.",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
