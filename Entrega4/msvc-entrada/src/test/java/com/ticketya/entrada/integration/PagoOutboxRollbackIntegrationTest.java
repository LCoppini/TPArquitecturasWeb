package com.ticketya.entrada.integration;

import com.ticketya.entrada.MsvcEntradaApplication;
import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.service.PagoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifica contra el PostgreSQL del compose que un rollback del pago no deja pendientes. */
@EnabledIfEnvironmentVariable(named = "ENTRADA_DB_INTEGRATION", matches = "true")
class PagoOutboxRollbackIntegrationTest {

    private ConfigurableApplicationContext contexto;
    private JdbcTemplate jdbc;
    private TransactionTemplate transaccion;
    private PagoService pagoService;

    @BeforeEach
    void levantarContexto() {
        contexto = new SpringApplicationBuilder(MsvcEntradaApplication.class)
                .properties(
                        "spring.cloud.config.enabled=false",
                        "eureka.client.enabled=false",
                        "spring.main.web-application-type=none",
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/ticketya_entrada",
                        "spring.datasource.username=ticketya",
                        "spring.datasource.password=ticketya",
                        "spring.jpa.hibernate.ddl-auto=update",
                        "ticketya.pasarela.resultado=APRUEBA",
                        "spring.main.banner-mode=off",
                        "logging.level.root=WARN")
                .web(WebApplicationType.NONE)
                .run();
        jdbc = contexto.getBean(JdbcTemplate.class);
        transaccion = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        pagoService = contexto.getBean(PagoService.class);
        limpiar();
    }

    @AfterEach
    void cerrarContexto() {
        limpiar();
        contexto.close();
    }

    @Test
    void rollbackDelPagoNoDejaElPendiente() {
        jdbc.update("""
                INSERT INTO entradas (id, id_evento, id_sector, id_usuario, estado, precio,
                                      expira_en, clave_idempotencia)
                VALUES (500, 20, 30, 42, 'RESERVADA', 100,
                        now() + interval '10 minutes', 'rollback-1')
                """);

        transaccion.execute(status -> {
            pagoService.crear(solicitud());
            assertEquals(1, contarPendientes());
            status.setRollbackOnly();
            return null;
        });

        assertEquals(0, contarPendientes());
    }

    private void limpiar() {
        jdbc.update("DELETE FROM pendientes_preferencia WHERE id_entrada = 500");
        jdbc.update("DELETE FROM registros_pago WHERE id_entrada = 500");
        jdbc.update("DELETE FROM pagos WHERE id_entrada = 500");
        jdbc.update("DELETE FROM entradas WHERE id = 500");
    }

    private int contarPendientes() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM pendientes_preferencia WHERE id_entrada = 500", Integer.class);
    }

    private PagoRequest solicitud() {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(500L);
        request.setIdUsuario(42L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("rollback-1");
        return request;
    }
}
