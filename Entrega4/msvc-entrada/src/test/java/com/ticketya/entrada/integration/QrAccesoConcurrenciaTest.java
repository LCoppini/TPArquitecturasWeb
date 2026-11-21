package com.ticketya.entrada.integration;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.exception.QrError;
import com.ticketya.entrada.exception.QrRechazadoException;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.service.EntradaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EntradaService.class)
@Testcontainers
class QrAccesoConcurrenciaTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private EntradaService entradaService;

    @Autowired
    private EntradaRepository entradaRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private EventoClient eventoClient;

    @MockitoBean
    private SectorClient sectorClient;

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosEscaneosConcurrentesDelMismoQrAceptanSoloUno() throws Exception {
        Entrada entrada = new Entrada();
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setIdSector(30L);
        entrada.setPrecio(BigDecimal.TEN);
        entrada.setCodigoQr("qr-concurrente");
        entrada.setEstado("VALIDA");
        entradaRepository.saveAndFlush(entrada);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> primero = ejecutor.submit(escanear(inicio, 101L));
            Future<Object> segundo = ejecutor.submit(escanear(inicio, 102L));
            inicio.countDown();

            Object resultadoUno = primero.get();
            Object resultadoDos = segundo.get();

            long aprobados = contar(resultadoUno, Long.class);
            long rechazados = contar(resultadoUno, QrError.class) + contar(resultadoDos, QrError.class);
            assertEquals(1, aprobados + contar(resultadoDos, Long.class));
            assertEquals(1, rechazados);
            Object rechazo = resultadoUno instanceof QrError ? resultadoUno : resultadoDos;
            assertEquals(QrError.QR_YA_USADO, rechazo);

            Long personalGanador = resultadoUno instanceof Long
                    ? (Long) resultadoUno
                    : (Long) resultadoDos;

            entityManager.clear();
            Entrada guardada = entradaRepository.findByCodigoQr("qr-concurrente").orElseThrow();
            assertEquals("USADA", guardada.getEstado());
            assertNotNull(guardada.getIngresadaEn());
            assertEquals(personalGanador, guardada.getIdPersonalAcceso());
        } finally {
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void codigoQrDuplicadoSeRechazaEnLaBase() {
        entradaRepository.saveAndFlush(entrada("qr-duplicado"));

        assertThrows(DataIntegrityViolationException.class,
                () -> entradaRepository.saveAndFlush(entrada("qr-duplicado")));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void variasReservasSinQrNoColisionanPorLaRestriccionUnica() {
        long cantidadInicial = entradaRepository.count();
        assertDoesNotThrow(() -> {
            entradaRepository.saveAndFlush(entrada(null));
            entradaRepository.saveAndFlush(entrada(null));
        });
        assertEquals(cantidadInicial + 2, entradaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelacionDeEventoRegistraElMotivoEnLaEntrada() {
        entradaRepository.saveAndFlush(entrada("qr-cancelado"));

        entradaService.cancelarEvento(20L);

        entityManager.clear();
        Entrada guardada = entradaRepository.findByCodigoQr("qr-cancelado").orElseThrow();
        assertEquals("EN_REINTEGRO", guardada.getEstado());
        assertEquals("CANCELACION", guardada.getMotivoInvalidacion());
    }

    private Entrada entrada(String codigoQr) {
        Entrada entrada = new Entrada();
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setIdSector(30L);
        entrada.setPrecio(BigDecimal.TEN);
        entrada.setCodigoQr(codigoQr);
        entrada.setEstado("VALIDA");
        return entrada;
    }

    private Callable<Object> escanear(CountDownLatch inicio, Long idPersonal) {
        return () -> {
            inicio.await();
            try {
                entradaService.validarQr("qr-concurrente", 20L, idPersonal);
                return idPersonal;
            } catch (QrRechazadoException exception) {
                return exception.getError();
            }
        };
    }

    private long contar(Object resultado, Class<?> tipo) {
        return tipo.isInstance(resultado) ? 1 : 0;
    }
}
