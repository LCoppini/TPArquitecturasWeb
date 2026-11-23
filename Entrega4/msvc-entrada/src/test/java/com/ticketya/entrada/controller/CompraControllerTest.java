package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.exception.ApiExceptionHandler;
import com.ticketya.entrada.service.CompraService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompraControllerTest {

    private static final String CUERPO_COMPRA = """
            {"idEvento":20,"entradas":[{"idSector":30,"fila":null,"asiento":null}]}
            """;

    @Mock
    private CompraService compraService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CompraController(compraService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void crearCompraDevuelve201ConLaReserva() throws Exception {
        when(compraService.crear(eq(10L), eq("clave-1"), any())).thenReturn(compra("RESERVADA", null));

        mockMvc.perform(post("/api/compras")
                        .header("X-User-Id", 10L)
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPRA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCompra").value(500L))
                .andExpect(jsonPath("$.estado").value("RESERVADA"));
    }

    @Test
    void crearCompraConCupoInsuficienteDevuelve409ConElCodigo() throws Exception {
        when(compraService.crear(eq(10L), eq("clave-2"), any()))
                .thenReturn(compra(null, "CUPO_TANDA_INSUFICIENTE"));

        mockMvc.perform(post("/api/compras")
                        .header("X-User-Id", 10L)
                        .header("Idempotency-Key", "clave-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPRA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUPO_TANDA_INSUFICIENTE"))
                .andExpect(jsonPath("$.cantidadDisponible").value(1));
    }

    @Test
    void crearCompraRequiereLasDosCabeceras() throws Exception {
        mockMvc.perform(post("/api/compras")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPRA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/compras")
                        .header("Idempotency-Key", "clave-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPRA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCompraConMasDeSeisEntradasDevuelve400() throws Exception {
        String sieteEntradas = IntStream.range(0, 7)
                .mapToObj(indice -> "{\"idSector\":30,\"fila\":null,\"asiento\":null}")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/compras")
                        .header("X-User-Id", 10L)
                        .header("Idempotency-Key", "clave-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEvento\":20,\"entradas\":[" + sieteEntradas + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void consultarCompraDevuelveEstadoYCupo() throws Exception {
        when(compraService.consultar(500L, 10L)).thenReturn(compra("PAGADA", null));

        mockMvc.perform(get("/api/compras/{idCompra}", 500L).header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
    }

    @Test
    void consultarCompraAjenaDevuelve404() throws Exception {
        when(compraService.consultar(500L, 11L))
                .thenThrow(new EntityNotFoundException("No se encontró la Compra"));

        mockMvc.perform(get("/api/compras/{idCompra}", 500L).header("X-User-Id", 11L))
                .andExpect(status().isNotFound());
    }

    @Test
    void solicitarDevolucionDevuelve202() throws Exception {
        mockMvc.perform(post("/api/compras/{idCompra}/devolucion", 500L).header("X-User-Id", 10L))
                .andExpect(status().isAccepted());

        verify(compraService).solicitarDevolucion(500L, 10L);
    }

    @Test
    void pagarCompraDevuelve200ConElTotal() throws Exception {
        when(compraService.pagar(eq(500L), eq(10L), eq("pago-1"), any(PagoCompraRequest.class)))
                .thenReturn(new PagoCompraResponse(600L, 500L, 10L, "PAGADA", "TARJETA",
                        new BigDecimal("25.00"), "ref-1"));

        mockMvc.perform(post("/api/compras/{idCompra}/pago", 500L)
                        .header("X-User-Id", 10L)
                        .header("Idempotency-Key", "pago-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioDePago\":\"TARJETA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.total").value(25.00));
    }

    @Test
    void pagarCompraVencidaDevuelve409SinCargo() throws Exception {
        when(compraService.pagar(eq(500L), eq(10L), eq("pago-2"), any(PagoCompraRequest.class)))
                .thenReturn(new PagoCompraResponse(null, 500L, 10L, "VENCIDA", "TARJETA",
                        BigDecimal.ZERO, null));

        mockMvc.perform(post("/api/compras/{idCompra}/pago", 500L)
                        .header("X-User-Id", 10L)
                        .header("Idempotency-Key", "pago-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioDePago\":\"TARJETA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value("VENCIDA"))
                .andExpect(jsonPath("$.idPago").doesNotExist());
    }

    private CompraResponse compra(String estado, String codigo) {
        return new CompraResponse(500L, 20L, estado, new BigDecimal("25.00"), null, List.of(),
                codigo, codigo == null ? null : 1);
    }
}
