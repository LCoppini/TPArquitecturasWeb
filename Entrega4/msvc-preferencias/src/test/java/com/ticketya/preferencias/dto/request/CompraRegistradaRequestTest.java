package com.ticketya.preferencias.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraRegistradaRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void inicializarValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidator() {
        factory.close();
    }

    @Test
    void compraValidaNoTieneViolaciones() {
        CompraRegistradaRequest request = new CompraRegistradaRequest(
                42L, 101L, 20L, "SHOW", "Tandil",
                List.of(new ArtistaCompraRequest(7L, "Banda")));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void camposObligatoriosDelContratoNoPuedenSerNulosNiVacios() {
        CompraRegistradaRequest request = new CompraRegistradaRequest(
                null, null, null, "", " ", List.of(new ArtistaCompraRequest(null, "")));

        Set<String> campos = validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("idUsuario", "idEntrada", "idEvento", "tipoEvento", "ciudad",
                        "artistas[0].idArtista", "artistas[0].nombre"),
                campos);
    }

    @Test
    void artistaNuloDentroDeLaListaSeValida() {
        CompraRegistradaRequest request = new CompraRegistradaRequest(
                42L, 101L, 20L, "SHOW", "Tandil",
                java.util.Collections.singletonList(null));

        Set<ConstraintViolation<CompraRegistradaRequest>> violaciones = validator.validate(request);

        assertEquals(1, violaciones.size());
        assertTrue(violaciones.iterator().next().getPropertyPath().toString().contains("artistas"));
    }
}
