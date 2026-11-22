package com.ticketya.entrada.integration;

import com.ticketya.entrada.MsvcEntradaApplication;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dos contextos Spring (dos "instancias") contra el PostgreSQL levantado con
 * docker compose. Se habilita con ENTRADA_DB_INTEGRATION=true; ADR-0002, sin Testcontainers.
 */
@EnabledIfEnvironmentVariable(named = "ENTRADA_DB_INTEGRATION", matches = "true")
class PendientePreferenciaClaimIntegrationTest {

    private static final String[] PROPIEDADES = {
            "spring.cloud.config.enabled=false",
            "eureka.client.enabled=false",
            "spring.main.web-application-type=none",
            "spring.datasource.url=jdbc:postgresql://localhost:5432/ticketya_entrada",
            "spring.datasource.username=ticketya",
            "spring.datasource.password=ticketya",
            "spring.jpa.hibernate.ddl-auto=update",
            "spring.main.banner-mode=off",
            "logging.level.root=WARN"
    };

    private ConfigurableApplicationContext instanciaUno;
    private ConfigurableApplicationContext instanciaDos;
    private JdbcTemplate jdbc;
    private PendientePreferenciaRepository repositorioUno;
    private PendientePreferenciaRepository repositorioDos;
    private ExecutorService hilos;

    @BeforeEach
    void levantarDosInstancias() {
        instanciaUno = nuevaInstancia();
        instanciaDos = nuevaInstancia();
        jdbc = instanciaUno.getBean(JdbcTemplate.class);
        repositorioUno = instanciaUno.getBean(PendientePreferenciaRepository.class);
        repositorioDos = instanciaDos.getBean(PendientePreferenciaRepository.class);
        hilos = Executors.newFixedThreadPool(2);
        jdbc.update("DELETE FROM pendientes_preferencia");
    }

    @AfterEach
    void cerrar() {
        if (hilos != null) {
            hilos.shutdownNow();
        }
        if (jdbc != null) {
            jdbc.update("DELETE FROM pendientes_preferencia");
        }
        if (instanciaDos != null) {
            instanciaDos.close();
        }
        if (instanciaUno != null) {
            instanciaUno.close();
        }
    }

    @Test
    void dosInstanciasNoReclamanLaMismaFila() throws Exception {
        insertar(101L, "corr-a");
        insertar(102L, "corr-b");
        CountDownLatch inicio = new CountDownLatch(1);
        Instant ahora = Instant.now();
        Instant stale = ahora.minus(5, ChronoUnit.MINUTES);

        Future<Long> reclamoUno = hilos.submit(() -> reclamar(repositorioUno, inicio, ahora, stale));
        Future<Long> reclamoDos = hilos.submit(() -> reclamar(repositorioDos, inicio, ahora, stale));
        inicio.countDown();

        Long idUno = reclamoUno.get(30, TimeUnit.SECONDS);
        Long idDos = reclamoDos.get(30, TimeUnit.SECONDS);

        assertNotNull(idUno);
        assertNotNull(idDos);
        assertNotEquals(idUno, idDos);
        assertEquals(Set.of(idUno, idDos), Set.of(idDeEntrada(101L), idDeEntrada(102L)));
        assertEquals(2, contarEnProceso());
    }

    @Test
    void unRegistroEnProcesoStaleVuelveASerElegible() {
        insertar(201L, "corr-stale");
        Long id = idDeEntrada(201L);
        Instant haceDiezMinutos = Instant.now().minus(10, ChronoUnit.MINUTES);
        jdbc.update("UPDATE pendientes_preferencia SET estado = 'EN_PROCESO', actualizado_en = ? WHERE id = ?",
                java.sql.Timestamp.from(haceDiezMinutos), id);

        PendientePreferencia reclamado = repositorioUno
                .reclamarSiguiente(Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES))
                .orElseThrow();

        assertEquals(id, reclamado.getId());
        assertEquals(PendientePreferencia.EN_PROCESO, reclamado.getEstado());
        assertTrue(reclamado.getActualizadoEn().isAfter(haceDiezMinutos));
    }

    @Test
    void elResultadoSoloSeGuardaSiLaFilaSigueEnProceso() {
        insertar(301L, "corr-resultado");
        Long id = idDeEntrada(301L);
        PendientePreferencia reclamado = repositorioUno
                .reclamarSiguiente(Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES))
                .orElseThrow();
        Instant proximo = Instant.now().plusSeconds(10);

        int primera = repositorioUno.guardarResultado(id, PendientePreferencia.FALLIDO, 1,
                proximo, "boom", Instant.now());
        int segunda = repositorioDos.guardarResultado(id, PendientePreferencia.ENVIADO, 2,
                null, null, Instant.now());

        assertEquals(1, primera);
        assertEquals(0, segunda);
        PendientePreferencia persistido = repositorioDos.findById(id).orElseThrow();
        assertEquals(PendientePreferencia.FALLIDO, persistido.getEstado());
        assertEquals(1, persistido.getIntentos());
        assertEquals("boom", persistido.getUltimoError());
        assertEquals(proximo.toEpochMilli(), persistido.getProximoIntentoEn().toEpochMilli());
    }

    @Test
    void laUnicidadDeIdEntradaEvitaAvisosDuplicados() {
        insertar(401L, "corr-unico");

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> insertar(401L, "corr-otro"));
    }

    private ConfigurableApplicationContext nuevaInstancia() {
        return new SpringApplicationBuilder(MsvcEntradaApplication.class)
                .properties(PROPIEDADES)
                .web(WebApplicationType.NONE)
                .run();
    }

    private Long reclamar(PendientePreferenciaRepository repositorio, CountDownLatch inicio,
                          Instant ahora, Instant stale) throws InterruptedException {
        inicio.await();
        return repositorio.reclamarSiguiente(ahora, stale)
                .map(PendientePreferencia::getId)
                .orElse(null);
    }

    private void insertar(Long idEntrada, String correlationId) {
        Instant ahora = Instant.now();
        jdbc.update("""
                        INSERT INTO pendientes_preferencia
                        (id_entrada, id_usuario, id_evento, correlation_id, estado, intentos,
                         creado_en, actualizado_en)
                        VALUES (?, ?, ?, ?, 'PENDIENTE', 0, ?, ?)
                        """,
                idEntrada, 42L, 20L, correlationId,
                java.sql.Timestamp.from(ahora), java.sql.Timestamp.from(ahora));
    }

    private Long idDeEntrada(Long idEntrada) {
        return jdbc.queryForObject("SELECT id FROM pendientes_preferencia WHERE id_entrada = ?",
                Long.class, idEntrada);
    }

    private int contarEnProceso() {
        List<Integer> conteos = new ArrayList<>(jdbc.queryForList(
                "SELECT COUNT(*) FROM pendientes_preferencia WHERE estado = 'EN_PROCESO'", Integer.class));
        return conteos.isEmpty() ? 0 : conteos.get(0);
    }
}
