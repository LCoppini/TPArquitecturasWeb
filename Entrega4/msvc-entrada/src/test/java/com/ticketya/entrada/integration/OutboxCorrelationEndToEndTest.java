package com.ticketya.entrada.integration;

import com.ticketya.entrada.MsvcEntradaApplication;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.service.PagoService;
import com.ticketya.entrada.service.impl.PreferenciasOutboxScheduler;
import feign.Client;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.MDC;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación de punta a punta del outbox (7.2): dos contextos Spring contra el
 * PostgreSQL levantado por docker compose. El cliente Feign se sustituye por un stub
 * que captura las request reales, incluido el header X-Correlation-Id que agrega el
 * interceptor; no hace falta levantar los otros microservicios (ADR-0002).
 */
@EnabledIfEnvironmentVariable(named = "ENTRADA_DB_INTEGRATION", matches = "true")
class OutboxCorrelationEndToEndTest {

    private static final String CORRELATION_ID = "corr-e2e-72";
    private static final Long ID_ENTRADA = 600L;
    private static final List<Request> SOLICITUDES = Collections.synchronizedList(new ArrayList<>());
    private static volatile CountDownLatch postPreferencias;

    private ConfigurableApplicationContext instanciaUno;
    private ConfigurableApplicationContext instanciaDos;
    private JdbcTemplate jdbc;
    private ExecutorService hilos;

    @BeforeEach
    void levantarDosInstancias() {
        SOLICITUDES.clear();
        postPreferencias = new CountDownLatch(1);
        instanciaUno = nuevaInstancia();
        instanciaDos = nuevaInstancia();
        jdbc = instanciaUno.getBean(JdbcTemplate.class);
        hilos = Executors.newFixedThreadPool(2);
        limpiar();
    }

    @AfterEach
    void cerrar() {
        if (hilos != null) {
            hilos.shutdownNow();
        }
        if (jdbc != null) {
            limpiar();
        }
        MDC.clear();
        if (instanciaDos != null) {
            instanciaDos.close();
        }
        if (instanciaUno != null) {
            instanciaUno.close();
        }
    }

    @Test
    void pagoAprobadoCorrelationIdClaimUnicoYRecuperacionDeStale() throws Exception {
        // 1. El pago aprobado guarda el correlation id del request en la fila del outbox.
        jdbc.update("""
                INSERT INTO entradas (id, id_evento, id_sector, id_usuario, estado, precio,
                                      expira_en, clave_idempotencia)
                VALUES (?, 20, 30, 42, 'RESERVADA', 100, now() + interval '10 minutes', 'e2e-72')
                """, ID_ENTRADA);
        MDC.put(CorrelationContext.MDC_KEY, CORRELATION_ID);
        instanciaUno.getBean(PagoService.class).crear(solicitud());
        MDC.clear();

        assertEquals(CORRELATION_ID, jdbc.queryForObject(
                "SELECT correlation_id FROM pendientes_preferencia WHERE id_entrada = ?",
                String.class, ID_ENTRADA));
        assertEquals("PENDIENTE", estadoDelPendiente());

        // 2. Dos instancias despachan a la vez: solo una reclama y envía la compra.
        CountDownLatch inicio = new CountDownLatch(1);
        Future<?> despachoUno = hilos.submit(() -> despachar(instanciaUno, inicio));
        Future<?> despachoDos = hilos.submit(() -> despachar(instanciaDos, inicio));
        inicio.countDown();
        despachoUno.get(30, TimeUnit.SECONDS);
        despachoDos.get(30, TimeUnit.SECONDS);

        assertTrue(postPreferencias.await(30, TimeUnit.SECONDS), "Ningún worker envió la compra");
        List<Request> envios = SOLICITUDES.stream()
                .filter(peticion -> peticion.url().contains("/internal/preferencias/compras"))
                .toList();
        assertEquals(1, envios.size(), "Solo una instancia debe reclamar cada pendiente");
        Request envio = envios.get(0);
        assertEquals(CORRELATION_ID, primerValor(envio, CorrelationContext.HEADER));
        String cuerpo = new String(envio.body(), StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains("\"idEntrada\":" + ID_ENTRADA));
        assertTrue(cuerpo.contains("\"tipoEvento\":\"SHOW\""));
        assertTrue(cuerpo.contains("\"ciudad\":\"Tandil\""));
        esperarEstado("ENVIADO");

        // El mismo correlation id llega en las consultas de enriquecimiento del worker.
        assertTrue(SOLICITUDES.stream()
                .filter(peticion -> peticion.url().contains("/api/eventos/"))
                .allMatch(peticion -> CORRELATION_ID.equals(
                        primerValor(peticion, CorrelationContext.HEADER))));
        assertTrue(SOLICITUDES.stream()
                .filter(peticion -> peticion.url().contains("/api/usuarios/"))
                .allMatch(peticion -> CORRELATION_ID.equals(
                        primerValor(peticion, CorrelationContext.HEADER))));

        // 3. Un EN_PROCESO abandonado vuelve a ser elegible para la otra instancia.
        jdbc.update("""
                UPDATE pendientes_preferencia
                SET estado = 'EN_PROCESO', actualizado_en = ?
                WHERE id_entrada = ?
                """, Timestamp.from(Instant.now().minus(10, ChronoUnit.MINUTES)), ID_ENTRADA);
        Instant ahora = Instant.now();
        Long reclamado = instanciaDos.getBean(PendientePreferenciaRepository.class)
                .reclamarSiguiente(ahora, ahora.minus(5, ChronoUnit.MINUTES))
                .map(pendiente -> pendiente.getIdEntrada())
                .orElseThrow();
        assertEquals(ID_ENTRADA, reclamado);
    }

    private void despachar(ConfigurableApplicationContext contexto, CountDownLatch inicio) {
        try {
            inicio.await();
            contexto.getBean(PreferenciasOutboxScheduler.class).reclamarYDespachar();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private ConfigurableApplicationContext nuevaInstancia() {
        return new SpringApplicationBuilder(MsvcEntradaApplication.class, StubFeignClientConfig.class)
                .properties(
                        "spring.cloud.config.enabled=false",
                        "eureka.client.enabled=false",
                        "spring.main.web-application-type=none",
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/ticketya_entrada",
                        "spring.datasource.username=ticketya",
                        "spring.datasource.password=ticketya",
                        "spring.jpa.hibernate.ddl-auto=update",
                        "ticketya.preferencias.outbox.poll-ms=1h",
                        "spring.cloud.openfeign.client.config.msvc-evento.url=http://stub-evento",
                        "spring.cloud.openfeign.client.config.msvc-usuarios.url=http://stub-usuarios",
                        "spring.cloud.openfeign.client.config.msvc-preferencias.url=http://stub-preferencias",
                        "spring.main.banner-mode=off",
                        "logging.level.root=WARN")
                .web(WebApplicationType.NONE)
                .run();
    }

    private void esperarEstado(String esperado) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        String estado;
        do {
            estado = estadoDelPendiente();
            if (esperado.equals(estado)) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        } while (System.nanoTime() < limite);
        assertEquals(esperado, estado, "El pendiente no alcanzó el estado esperado");
    }

    private String primerValor(Request peticion, String cabecera) {
        return peticion.headers().get(cabecera).iterator().next();
    }

    private String estadoDelPendiente() {
        return jdbc.queryForObject(
                "SELECT estado FROM pendientes_preferencia WHERE id_entrada = ?",
                String.class, ID_ENTRADA);
    }

    private PagoRequest solicitud() {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(ID_ENTRADA);
        request.setIdUsuario(42L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("e2e-72");
        return request;
    }

    private void limpiar() {
        jdbc.update("DELETE FROM pendientes_preferencia WHERE id_entrada = ?", ID_ENTRADA);
        jdbc.update("DELETE FROM registros_pago WHERE id_entrada = ?", ID_ENTRADA);
        jdbc.update("DELETE FROM pagos WHERE id_entrada = ?", ID_ENTRADA);
        jdbc.update("DELETE FROM entradas WHERE id = ?", ID_ENTRADA);
    }

    /** Cliente Feign sustituto: captura las request reales, con los headers del interceptor. */
    @Configuration
    static class StubFeignClientConfig {

        @Bean
        Client clienteFeignStub() {
            return (request, options) -> {
                SOLICITUDES.add(request);
                String url = request.url();
                String cuerpo;
                int estado = 200;
                if (url.contains("/internal/preferencias/compras")) {
                    estado = 201;
                    cuerpo = "";
                    postPreferencias.countDown();
                } else if (url.contains("/api/eventos/")) {
                    cuerpo = """
                            {"id":20,"fechaHora":"2026-11-22T19:00:00Z","estado":"PUBLICADO",
                             "tipo":"SHOW","participantes":[{"id":7,"nombre":"Banda","tipo":"Banda"}]}
                            """;
                } else if (url.contains("/api/usuarios/")) {
                    cuerpo = "{\"idUsuario\":42,\"ciudad\":\"Tandil\"}";
                } else {
                    estado = 404;
                    cuerpo = "{}";
                }
                return Response.builder()
                        .status(estado)
                        .request(request)
                        .headers(Map.of("Content-Type", List.of("application/json")))
                        .body(cuerpo, StandardCharsets.UTF_8)
                        .build();
            };
        }
    }
}
