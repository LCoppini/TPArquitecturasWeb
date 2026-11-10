package com.ticketya.gateway.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class GatewayErrorController implements ErrorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayErrorController.class);

    @RequestMapping
    public ResponseEntity<GatewayErrorResponse> error(HttpServletRequest request) {
        int status = statusFrom(request);
        Throwable exception = exceptionFrom(request);

        // El filtro lb de Gateway MVC envía al error dispatcher una excepción HTTP 503.
        if (servicioNoDisponible(exception)) {
            status = HttpStatus.SERVICE_UNAVAILABLE.value();
            LOGGER.warn("No se encontró una instancia disponible para {}", pathFrom(request));
        }

        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null || !httpStatus.isError()) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        GatewayErrorResponse response = new GatewayErrorResponse(
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                mensajePublico(httpStatus),
                pathFrom(request)
        );
        return ResponseEntity.status(httpStatus).body(response);
    }

    private int statusFrom(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return status instanceof Integer code ? code : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private Throwable exceptionFrom(HttpServletRequest request) {
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        return exception instanceof Throwable throwable ? throwable : null;
    }

    private boolean servicioNoDisponible(Throwable exception) {
        Throwable actual = exception;
        while (actual != null) {
            if (actual instanceof ResourceAccessException) {
                return true;
            }
            if (actual instanceof HttpServerErrorException serverException
                    && serverException.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private String pathFrom(HttpServletRequest request) {
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return path instanceof String value ? value : request.getRequestURI();
    }

    private String mensajePublico(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No se encontró una ruta para la solicitud.";
            case SERVICE_UNAVAILABLE -> "El servicio solicitado no está disponible.";
            default -> status.is5xxServerError()
                    ? "Ocurrió un error interno en el gateway."
                    : "La solicitud no pudo ser procesada.";
        };
    }
}
