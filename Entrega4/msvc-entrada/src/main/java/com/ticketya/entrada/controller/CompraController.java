package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.entity.Compra;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ticketya.entrada.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compras")
@Tag(name = "Compra agrupada", description = "Reserva, pago, consulta y devolución de hasta seis entradas por Compra")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @Operation(
            summary = "Reservar entradas",
            description = "Idempotente por Idempotency-Key. 409 con codigo CUPO_TANDA_INSUFICIENTE o COMPRA_PENDIENTE; 422 si la misma clave se reutiliza con otro payload."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra RESERVADA creada"),
            @ApiResponse(responseCode = "400", description = "Faltan cabeceras o el payload es inválido"),
            @ApiResponse(responseCode = "409", description = "Cupo insuficiente, Compra pendiente o sin turno vigente"),
            @ApiResponse(responseCode = "422", description = "Idempotency-Key reutilizada con distinto payload"),
            @ApiResponse(responseCode = "503", description = "Fila deshabilitada por configuración")
    })
    @PostMapping
    public ResponseEntity<CompraResponse> crear(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody CompraRequest request
    ) {
        CompraResponse compra = compraService.crear(idUsuario, clave, request);
        return compra.codigo() == null
                ? ResponseEntity.status(HttpStatus.CREATED).body(compra)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(compra);
    }

    @Operation(summary = "Consultar una Compra", description = "Solo el dueño de la Compra puede consultarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compra del usuario"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "404", description = "Compra ajena o inexistente")
    })
    @GetMapping("/{idCompra}")
    public CompraResponse consultar(
            @PathVariable Long idCompra,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        return compraService.consultar(idCompra, idUsuario);
    }

    @Operation(
            summary = "Solicitar devolución completa",
            description = "Requiere Compra PAGADA, sin entradas usadas y al menos 72 horas antes del evento. Resuelve en forma asíncrona."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Devolución solicitada"),
            @ApiResponse(responseCode = "400", description = "Falta X-User-Id"),
            @ApiResponse(responseCode = "404", description = "Compra ajena o inexistente"),
            @ApiResponse(responseCode = "409", description = "La Compra no admite devolución completa")
    })
    @PostMapping("/{idCompra}/devolucion")
    public ResponseEntity<Void> solicitarDevolucion(
            @PathVariable Long idCompra,
            @RequestHeader("X-User-Id") Long idUsuario
    ) {
        compraService.solicitarDevolucion(idCompra, idUsuario);
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "Pagar una Compra agrupada",
            description = "Idempotente por Idempotency-Key. APRUEBA emite todas las entradas; FALLA compensa y libera el turno; DEMORA queda PENDIENTE. estado es el estado agregado de la Compra."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago aprobado o pendiente"),
            @ApiResponse(responseCode = "400", description = "Faltan cabeceras o el payload es inválido"),
            @ApiResponse(responseCode = "404", description = "Compra ajena o inexistente"),
            @ApiResponse(responseCode = "409", description = "La Compra venció antes del pago"),
            @ApiResponse(responseCode = "422", description = "Idempotency-Key reutilizada con distinto payload")
    })
    @PostMapping("/{idCompra}/pago")
    public ResponseEntity<PagoCompraResponse> pagar(
            @PathVariable Long idCompra,
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody PagoCompraRequest request
    ) {
        PagoCompraResponse pago = compraService.pagar(idCompra, idUsuario, clave, request);
        return Compra.VENCIDA.equals(pago.estado())
                ? ResponseEntity.status(HttpStatus.CONFLICT).body(pago)
                : ResponseEntity.ok(pago);
    }
}
