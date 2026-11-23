package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.FilaEventoResponse;
import com.ticketya.entrada.dto.response.IngresoFilaResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contratos escritos en docs/api-fila-virtual.md contra controllers y DTOs reales. */
class DocumentacionApiTest {

    private static final Path DOCUMENTACION = Path.of("..", "docs", "api-fila-virtual.md");

    @Test
    void elDocumentoIncluyeTodasLasRutasImplementadas() throws IOException {
        String documento = Files.readString(DOCUMENTACION);
        List.of(
                "PUT /api/organizador/filas-virtuales/eventos/{idEvento}/activacion",
                "POST /api/organizador/filas-virtuales/eventos/{idEvento}/{operacion}",
                "POST /api/filas-virtuales/eventos/{idEvento}/ingresos",
                "GET /api/filas-virtuales/eventos/{idEvento}/mi-estado",
                "POST /api/compras",
                "POST /api/compras/{idCompra}/pago",
                "GET /api/compras/{idCompra}",
                "POST /api/compras/{idCompra}/devolucion"
        ).forEach(ruta -> assertTrue(documento.contains(ruta), "Ruta no documentada: " + ruta));
    }

    @Test
    void losCodigosDelDocumentoEstanEnLasAnotacionesOpenApi() throws IOException {
        String documento = Files.readString(DOCUMENTACION);
        codigosDelDocumento(documento, "### `PUT /api/organizador/filas-virtuales/eventos/{idEvento}/activacion`")
                .forEach(codigo -> assertTrue(
                        codigos("activar", AdminFilaVirtualController.class).contains(codigo),
                        "activacion no documenta " + codigo));
        codigosDelDocumento(documento, "### `POST /api/filas-virtuales/eventos/{idEvento}/ingresos`")
                .forEach(codigo -> assertTrue(
                        codigos("unirse", FilaVirtualController.class).contains(codigo),
                        "ingresos no documenta " + codigo));
        codigosDelDocumento(documento, "### `POST /api/compras`")
                .forEach(codigo -> assertTrue(
                        codigos("crear", CompraController.class).contains(codigo),
                        "crear no documenta " + codigo));
        codigosDelDocumento(documento, "### `POST /api/compras/{idCompra}/pago`")
                .forEach(codigo -> assertTrue(
                        codigos("pagar", CompraController.class).contains(codigo),
                        "pagar no documenta " + codigo));
    }

    @Test
    void losControllersEstanEtiquetadosYOperacionesDocumentadas() {
        assertTrue(AdminFilaVirtualController.class.isAnnotationPresent(Tag.class));
        assertTrue(FilaVirtualController.class.isAnnotationPresent(Tag.class));
        assertTrue(CompraController.class.isAnnotationPresent(Tag.class));
        for (Class<?> controller : List.of(AdminFilaVirtualController.class,
                FilaVirtualController.class, CompraController.class)) {
            assertTrue(Arrays.stream(controller.getDeclaredMethods())
                    .anyMatch(metodo -> metodo.isAnnotationPresent(Operation.class)),
                    controller.getSimpleName() + " sin @Operation");
        }
    }

    @Test
    void losEjemplosDeDtoUsanLosCamposReales() throws IOException {
        String documento = Files.readString(DOCUMENTACION);
        campos(ActivarFilaRequest.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(FilaEventoResponse.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(IngresoFilaResponse.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(CompraRequest.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(CompraResponse.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(PagoCompraRequest.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
        campos(PagoCompraResponse.class).forEach(campo -> assertTrue(documento.contains(campo), campo));
    }

    private Set<String> codigos(String nombre, Class<?> controller) {
        Method metodo = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidato -> candidato.getName().equals(nombre))
                .findFirst().orElseThrow();
        ApiResponses respuestas = metodo.getAnnotation(ApiResponses.class);
        return Arrays.stream(respuestas.value())
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet());
    }

    private List<String> codigosDelDocumento(String documento, String marca) throws IOException {
        int inicio = documento.indexOf(marca);
        assertTrue(inicio >= 0, "Sección no encontrada: " + marca);
        int fin = documento.indexOf("Códigos:", inicio);
        assertTrue(fin > inicio, "Sin línea de códigos tras " + marca);
        int salto = documento.indexOf('\n', fin);
        return Arrays.stream(documento.substring(fin, salto).replaceAll("\\D+", " ").split(" "))
                .filter(codigo -> codigo.matches("[245]\\d\\d"))
                .toList();
    }

    private List<String> campos(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
                .map(componente -> componente.getName())
                .toList();
    }
}
