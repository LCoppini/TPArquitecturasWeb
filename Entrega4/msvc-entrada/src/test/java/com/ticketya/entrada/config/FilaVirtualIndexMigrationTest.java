package com.ticketya.entrada.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilaVirtualIndexMigrationTest {

    @Test
    void creaIndiceUnicoParcialParaMembresiasVigentes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Integer.class)))
                .thenReturn(3);

        new FilaVirtualIndexMigration(jdbcTemplate).run(null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.atLeast(7)).execute(sql.capture());
        assertTrue(sql.getAllValues().getFirst().contains("pg_advisory_xact_lock(1346454346, 0)"));
        assertTrue(sql.getAllValues().stream().anyMatch(statement ->
                statement.contains("uk_entrada_fila_usuario_vigente")
                        && statement.contains("WHERE estado IN ('EN_ESPERA', 'HABILITADO')")));
        assertTrue(sql.getAllValues().stream().anyMatch(statement ->
                statement.contains("uk_compra_reservada_usuario_evento")
                        && statement.contains("WHERE estado = 'RESERVADA'")));
    }
}
