package com.ticketya.ubicacion.dto;

import com.ticketya.ubicacion.dto.request.LugarRequest;
import com.ticketya.ubicacion.dto.request.SectorRequest;
import com.ticketya.ubicacion.entity.TipoSector;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

class LugarRequestValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    private static Set<String> campos(LugarRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static SectorRequest sector() {
        return new SectorRequest("Campo", TipoSector.NO_NUMERADO, null, null, 100);
    }

    @Test
    void lugarValidoNoTieneErrores() {
        LugarRequest request = new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1, List.of(sector()));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void lugarSinSectoresSeRechaza() {
        LugarRequest request = new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1, List.of());

        assertTrue(campos(request).contains("sectores"));
    }

    @Test
    void coordenadasFueraDeRangoSeRechazanYSeInformaCadaCampo() {
        LugarRequest request = new LugarRequest("Estadio", "Calle 1", "Tandil", 91.0, -181.0, List.of(sector()));

        Set<String> campos = campos(request);

        assertTrue(campos.contains("latitud"));
        assertTrue(campos.contains("longitud"));
    }

    @Test
    void camposObligatoriosVaciosSeRechazan() {
        LugarRequest request = new LugarRequest(" ", "", null, null, null, List.of(sector()));

        Set<String> campos = campos(request);

        assertTrue(campos.containsAll(Set.of("nombre", "direccion", "ciudad", "latitud", "longitud")));
    }

    @Test
    void capacidadCeroONegativaSeRechazaEnElSector() {
        SectorRequest invalido = new SectorRequest("Campo", TipoSector.NO_NUMERADO, null, null, 0);
        LugarRequest request = new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1, List.of(invalido));

        assertTrue(campos(request).contains("sectores[0].capacidadMax"));
    }

    @Test
    void sectorSinTipoNiNombreSeRechaza() {
        SectorRequest invalido = new SectorRequest("", null, null, null, 100);
        LugarRequest request = new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1, List.of(invalido));

        Set<String> campos = campos(request);

        assertTrue(campos.contains("sectores[0].nombre"));
        assertTrue(campos.contains("sectores[0].tipo"));
    }
}
