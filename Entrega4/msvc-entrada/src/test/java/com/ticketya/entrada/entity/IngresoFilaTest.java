package com.ticketya.entrada.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngresoFilaTest {

    @Test
    void laSecuenciaSeAsignaUnaSolaVez() {
        IngresoFila ingreso = new IngresoFila();

        ingreso.setSecuencia(7L);
        ingreso.setSecuencia(7L);

        assertEquals(7L, ingreso.getSecuencia());
        assertThrows(IllegalStateException.class, () -> ingreso.setSecuencia(8L));
    }
}
