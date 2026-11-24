package com.ticketya.entrada.integration;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.EntradaService;
import com.ticketya.entrada.service.FilaVirtualService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "ticketya.pasarela.resultado=APRUEBA"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EntradaService.class, CompraService.class, FilaVirtualService.class})
@Testcontainers
class FilaVirtualLockingIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired private FilaVirtualService filaVirtualService;
    @Autowired private EntradaService entradaService;
    @Autowired private CompraService compraService;
    @Autowired private FilaEventoRepository filaEventoRepository;
    @Autowired private IngresoFilaRepository ingresoFilaRepository;
    @Autowired private TandaFilaRepository tandaFilaRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private CompraRepository compraRepository;
    @Autowired private IntentoCompraRepository intentoCompraRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private UsuarioClient usuarioClient;
    @MockitoBean private EventoClient eventoClient;
    @MockitoBean private SectorClient sectorClient;

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void prepararEscenario() {
        jdbcTemplate.execute("DELETE FROM registros_pago");
        jdbcTemplate.execute("DELETE FROM pagos");
        jdbcTemplate.execute("DELETE FROM entradas");
        jdbcTemplate.execute("DELETE FROM intento_compra");
        jdbcTemplate.execute("DELETE FROM compras");
        jdbcTemplate.execute("DELETE FROM entrada_fila");
        jdbcTemplate.execute("DELETE FROM tanda_fila");
        jdbcTemplate.execute("DELETE FROM fila_evento");
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_entrada_fila_usuario_vigente
                ON entrada_fila (id_evento, id_usuario)
                WHERE estado IN ('EN_ESPERA', 'HABILITADO')
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_compra_reservada_usuario_evento
                ON compras (id_usuario, id_evento)
                WHERE estado = 'RESERVADA'
                """);

        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, 90L, 90L, "Evento", null, Instant.now().plusSeconds(345_600), "PUBLICADO",
                null, List.of(), List.of(
                        new EventoSectorResponse(30L, new BigDecimal("25.00")),
                        new EventoSectorResponse(31L, new BigDecimal("35.00"))
                )
        ));
        when(eventoClient.obtenerPrecioSector(20L, 30L))
                .thenReturn(new EventoSectorResponse(30L, new BigDecimal("25.00")));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 4, 2, 2, "NUMERADO"));
        when(sectorClient.obtenerPorId(31L))
                .thenReturn(new SectorResponse(31L, 4, 2, 2, "NUMERADO"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void activacionYReservaIndividualSeSerializanSinEstadoInconsistente() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<String> activacion = ejecutor.submit(() -> {
                esperar(inicio);
                try {
                    filaVirtualService.activar(20L, 90L, new ActivarFilaRequest(2, 4));
                    return "ACTIVADA";
                } catch (ResponseStatusException conflicto) {
                    return "CONFLICTO_" + conflicto.getStatusCode().value();
                }
            });
            Future<String> reserva = ejecutor.submit(() -> {
                esperar(inicio);
                try {
                    entradaService.crear(solicitudReserva("carrera-activacion-reserva", 10L));
                    return "RESERVADA";
                } catch (ResponseStatusException conflicto) {
                    return "CONFLICTO_" + conflicto.getStatusCode().value();
                }
            });
            inicio.countDown();

            String resultadoActivacion = activacion.get(40, TimeUnit.SECONDS);
            String resultadoReserva = reserva.get(40, TimeUnit.SECONDS);

            boolean ganoActivacion = "ACTIVADA".equals(resultadoActivacion);
            boolean ganoReserva = "RESERVADA".equals(resultadoReserva);
            assertTrue(ganoActivacion ^ ganoReserva,
                    "Solo una operación puede ganar: activación=" + resultadoActivacion
                            + ", reserva=" + resultadoReserva);
            if (ganoActivacion) {
                assertEquals("CONFLICTO_409", resultadoReserva);
            } else {
                assertEquals("CONFLICTO_409", resultadoActivacion);
            }
            entityManager.clear();
            assertEquals(ganoActivacion ? 1 : 0, filaEventoRepository.count());
            assertEquals(ganoReserva ? 1 : 0, entradaRepository.count());
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void comprasMultiasientoEnOrdenInversoNoGeneranDeadlock() throws Exception {
        prepararFila(10, 8, 10L, 11L);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<CompraResponse> primera = ejecutor.submit(() -> crearAlMismoTiempo(
                    inicio, 10L, "multiasiento-a",
                    List.of(new EntradaCompraRequest(30L, 1, 1), new EntradaCompraRequest(31L, 1, 1))
            ));
            Future<CompraResponse> segunda = ejecutor.submit(() -> crearAlMismoTiempo(
                    inicio, 11L, "multiasiento-b",
                    List.of(new EntradaCompraRequest(31L, 1, 2), new EntradaCompraRequest(30L, 1, 2))
            ));
            inicio.countDown();

            assertNotNull(primera.get(40, TimeUnit.SECONDS).idCompra());
            assertNotNull(segunda.get(40, TimeUnit.SECONDS).idCompra());
            entityManager.clear();
            assertEquals(2, compraRepository.count());
            assertEquals(4, entradaRepository.count());
            assertEquals(0, intentoCompraRepository.count());
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private CompraResponse crearAlMismoTiempo(
            CountDownLatch inicio,
            Long idUsuario,
            String clave,
            List<EntradaCompraRequest> selecciones
    ) {
        esperar(inicio);
        return compraService.crear(idUsuario, clave, new CompraRequest(20L, selecciones));
    }

    private EntradaRequest solicitudReserva(String clave, Long idUsuario) {
        EntradaRequest request = new EntradaRequest();
        request.setIdEvento(20L);
        request.setIdUsuario(idUsuario);
        request.setIdSector(30L);
        request.setFila(1);
        request.setAsiento(1);
        request.setClaveIdempotencia(clave);
        return request;
    }

    private TandaFila prepararFila(int capacidad, int cupo, Long... usuarios) {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");
        fila.setMaxPersonas(usuarios.length);
        fila.setCupoEntradas(cupo);
        fila.setCapacidadTotal(capacidad);
        fila.setSiguienteSecuencia((long) usuarios.length + 1);
        filaEventoRepository.saveAndFlush(fila);

        TandaFila tanda = new TandaFila();
        tanda.setIdEvento(20L);
        tanda.setNumero(1L);
        tanda.setEstado("ABIERTA");
        tanda.setIniciadaEn(Instant.now());
        tanda.setVenceEn(Instant.now().plusSeconds(600));
        tanda.setCupoEfectivo(cupo);
        tanda.setCupoRestante(cupo);
        tanda = tandaFilaRepository.saveAndFlush(tanda);
        for (int indice = 0; indice < usuarios.length; indice++) {
            IngresoFila ingreso = new IngresoFila();
            ingreso.setIdEvento(20L);
            ingreso.setIdUsuario(usuarios[indice]);
            ingreso.setSecuencia((long) indice + 1);
            ingreso.setEstado("HABILITADO");
            ingreso.setIngresadoEn(Instant.now());
            ingreso.setVenceEn(tanda.getVenceEn());
            ingreso.setIdTanda(tanda.getId());
            ingresoFilaRepository.save(ingreso);
        }
        ingresoFilaRepository.flush();
        return tanda;
    }

    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout esperando la señal de inicio");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
