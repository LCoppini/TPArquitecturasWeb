package com.ticketya.entrada.integration;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.config.FilaVirtualIndexMigration;
import com.ticketya.entrada.config.FilaVirtualProperties;
import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.IntentoCompra;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.FilaVirtualService;
import com.ticketya.entrada.service.PagoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "ticketya.pasarela.resultado=APRUEBA"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CompraService.class, FilaVirtualService.class, FilaVirtualIndexMigration.class, PagoService.class})
@Testcontainers
class CompraConcurrenciaIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired private CompraService compraService;
    @Autowired private CompraRepository compraRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private FilaEventoRepository filaEventoRepository;
    @Autowired private IngresoFilaRepository ingresoFilaRepository;
    @Autowired private IntentoCompraRepository intentoCompraRepository;
    @Autowired private TandaFilaRepository tandaFilaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private FilaVirtualService filaVirtualService;
    @Autowired private RegistroPagoRepository registroPagoRepository;
    @Autowired private PagoService pagoService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicationRunner indexMigration;

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
    void prepararEsquemaYServicios() {
        limpiarDatos();
        FilaVirtualProperties habilitado = new FilaVirtualProperties();
        habilitado.setEnabled(true);
        compraService.setFilaVirtualProperties(habilitado);
        filaVirtualService.setFilaVirtualProperties(habilitado);
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
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_compra_idempotencia
                ON pagos (id_compra, clave_idempotencia)
                WHERE id_compra IS NOT NULL
                """);
        jdbcTemplate.execute("ALTER TABLE pagos ADD COLUMN IF NOT EXISTS id_compra bigint");
        jdbcTemplate.execute("ALTER TABLE pagos ALTER COLUMN id_entrada DROP NOT NULL");
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, null, 90L, "Evento", null, Instant.now().plusSeconds(345_600), "PUBLICADO",
                null, List.of(), List.of(new EventoSectorResponse(30L, new BigDecimal("25.00")))
        ));
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 10, null, null, "NO_NUMERADO")
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void migracionesConcurrentesSerializanLaCreacionDeIndices() throws Exception {
        jdbcTemplate.execute("DROP INDEX uk_entrada_fila_usuario_vigente");
        jdbcTemplate.execute("DROP INDEX uk_compra_reservada_usuario_evento");
        jdbcTemplate.execute("DROP INDEX uk_pago_compra_idempotencia");
        ApplicationArguments argumentos = new DefaultApplicationArguments(new String[0]);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> una = executor.submit(() -> correrMigracion(argumentos));
            Future<?> dos = executor.submit(() -> correrMigracion(argumentos));
            una.get();
            dos.get();
            assertEquals(3, jdbcTemplate.queryForObject("""
                    select count(*) from pg_indexes
                    where indexname in (
                        'uk_entrada_fila_usuario_vigente',
                        'uk_compra_reservada_usuario_evento',
                        'uk_pago_compra_idempotencia'
                    )
                    """, Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresPermiteUnaSolaMembresiaVigentePorEventoYUsuario() {
        insertarMembresia(1L, "EN_ESPERA");

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> insertarMembresia(2L, "HABILITADO"));

        assertTrue(error.getMostSpecificCause().getMessage()
                .contains("uk_entrada_fila_usuario_vigente"));
        assertEquals(1, ingresoFilaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresPermiteNuevaMembresiaCuandoLaAnteriorYaNoEstaVigente() {
        insertarMembresia(1L, "VENCIDO");

        insertarMembresia(2L, "EN_ESPERA");

        assertEquals(2, ingresoFilaRepository.count());
        assertEquals(2L, ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(
                20L, "EN_ESPERA", org.springframework.data.domain.PageRequest.of(0, 10)
        ).getFirst().getSecuencia());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresPermiteUnaSolaCompraReservadaPorEventoYUsuario() {
        insertarCompra("RESERVADA", "reservada-1");

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> insertarCompra("RESERVADA", "reservada-2"));

        assertTrue(error.getMostSpecificCause().getMessage()
                .contains("uk_compra_reservada_usuario_evento"));
        assertEquals(1, compraRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresPermiteNuevaCompraCuandoLaAnteriorYaNoEstaReservada() {
        insertarCompra(Compra.VENCIDA, "vencida-1");

        insertarCompra(Compra.RESERVADA, "reservada-2");

        assertEquals(2, compraRepository.count());
    }

    private void insertarMembresia(Long secuencia, String estado) {
        jdbcTemplate.update("""
                INSERT INTO entrada_fila (id_evento, id_usuario, secuencia, estado, ingresado_en, version)
                VALUES (20, 10, ?, ?, CURRENT_TIMESTAMP, 0)
                """, secuencia, estado);
    }

    private void insertarCompra(String estado, String clave) {
        jdbcTemplate.update("""
                INSERT INTO compras (id_usuario, id_evento, estado, creada_en, vence_en,
                                     cantidad_entradas, total, clave_idempotencia, huella_solicitud, version)
                VALUES (10, 20, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 25.00, ?, ?, 0)
                """, estado, clave, clave);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresCargaPagoLegacyConSoloEntradaYRechazaAmbasReferencias() {
        Long idEntrada = jdbcTemplate.queryForObject("""
                INSERT INTO entradas (id_evento, id_usuario, id_sector, estado, precio, clave_idempotencia)
                VALUES (20, 10, 30, 'PAGADA', 25.00, 'entrada-legacy')
                RETURNING id
                """, Long.class);
        jdbcTemplate.update("""
                INSERT INTO pagos (id_usuario, estado, medio_de_pago, id_entrada, clave_idempotencia)
                VALUES (10, 'APROBADO', 'TARJETA', ?, 'pago-legacy')
                """, idEntrada);

        assertEquals(1, pagoRepository.findByIdEntradaIn(List.of(idEntrada)).size());
        assertEquals("PAGADA", entradaRepository.findById(idEntrada).orElseThrow().getEstado());
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO pagos (id_usuario, estado, medio_de_pago, id_entrada, id_compra, clave_idempotencia)
                VALUES (10, 'APROBADO', 'TARJETA', ?, 500, 'pago-invalido')
                """, idEntrada));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void pagoLegacySePersisteConSoloEntradaYRegistroPorEntrada() {
        Long idEntrada = jdbcTemplate.queryForObject("""
                INSERT INTO entradas (id_evento, id_usuario, id_sector, estado, precio, expira_en, clave_idempotencia)
                VALUES (20, 10, 30, 'RESERVADA', 25.00, CURRENT_TIMESTAMP + INTERVAL '10 minutes', 'entrada-pago-legacy')
                RETURNING id
                """, Long.class);

        pagoService.crear(solicitudPagoLegacy(idEntrada));

        entityManager.clear();
        Entrada entrada = entradaRepository.findById(idEntrada).orElseThrow();
        assertEquals("VALIDA", entrada.getEstado());
        assertNull(entrada.getIdCompra());
        assertEquals(1, pagoRepository.count());
        var pago = pagoRepository.findAll().getFirst();
        assertEquals(idEntrada, pago.getIdEntrada());
        assertNull(pago.getIdCompra());
        assertEquals(1, registroPagoRepository.count());
    }

    private PagoRequest solicitudPagoLegacy(Long idEntrada) {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(idEntrada);
        request.setIdUsuario(10L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("pago-legacy-" + idEntrada);
        return request;
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void postgresRechazaDosPagosDeCompraConLaMismaClave() {
        jdbcTemplate.update("""
                INSERT INTO pagos (id_usuario, estado, medio_de_pago, id_compra, clave_idempotencia)
                VALUES (10, 'APROBADO', 'TARJETA', 500, 'pago-compra-1')
                """);

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO pagos (id_usuario, estado, medio_de_pago, id_compra, clave_idempotencia)
                        VALUES (10, 'APROBADO', 'TARJETA', 500, 'pago-compra-1')
                        """));

        assertTrue(error.getMostSpecificCause().getMessage()
                .contains("uk_pago_compra_idempotencia"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void comprasConcurrentesConMismaClaveDevuelvenLaMismaCompra() throws Exception {
        prepararFila(4, 4, 10L);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CompraResponse> una = executor.submit(() -> crearAlMismoTiempo(inicio, 10L, "misma-clave"));
            Future<CompraResponse> dos = executor.submit(() -> crearAlMismoTiempo(inicio, 10L, "misma-clave"));
            inicio.countDown();

            CompraResponse resultadoUno = una.get();
            CompraResponse resultadoDos = dos.get();
            assertEquals(resultadoUno.idCompra(), resultadoDos.idCompra());
            assertEquals(1, compraRepository.count());
            assertEquals(1, entradaRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void compraPendienteUnicaYUltimoCupoSeResuelvenAtomicamente() throws Exception {
        TandaFila tanda = prepararFila(2, 1, 10L, 11L);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CompraResponse> una = executor.submit(() -> crearAlMismoTiempo(inicio, 10L, "clave-10"));
            Future<CompraResponse> dos = executor.submit(() -> crearAlMismoTiempo(inicio, 11L, "clave-11"));
            inicio.countDown();

            CompraResponse resultadoUno = una.get();
            CompraResponse resultadoDos = dos.get();
            List<CompraResponse> resultados = List.of(resultadoUno, resultadoDos);
            assertEquals(1, resultados.stream().filter(r -> r.idCompra() != null).count());
            assertEquals(1, resultados.stream()
                    .filter(r -> "CUPO_TANDA_INSUFICIENTE".equals(r.codigo())).count());
            assertEquals(1, compraRepository.count());
            assertEquals(1, entradaRepository.count());
            assertEquals(1, intentoCompraRepository.count());
            entityManager.clear();
            assertEquals(0, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void seleccionOcupadaEnCuartaEntradaRevierteTodaLaCompraYElCupo() {
        TandaFila tanda = prepararFila(10, 6, 10L);
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 10, 3, 3, "NUMERADO"));
        Entrada ocupada = new Entrada();
        ocupada.setIdEvento(20L);
        ocupada.setIdUsuario(99L);
        ocupada.setIdSector(30L);
        ocupada.setFila(2);
        ocupada.setAsiento(1);
        ocupada.setPrecio(new BigDecimal("25.00"));
        ocupada.setEstado("VALIDA");
        entradaRepository.saveAndFlush(ocupada);
        CompraRequest seisEntradas = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, 1, 1),
                new EntradaCompraRequest(30L, 1, 2),
                new EntradaCompraRequest(30L, 1, 3),
                new EntradaCompraRequest(30L, 2, 1),
                new EntradaCompraRequest(30L, 2, 2),
                new EntradaCompraRequest(30L, 2, 3)
        ));

        assertThrows(ResponseStatusException.class,
                () -> compraService.crear(10L, "asiento-ocupado-cuarto", seisEntradas));

        entityManager.clear();
        assertEquals(0, compraRepository.count());
        assertEquals(1, entradaRepository.count());
        assertEquals(6, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rechazoDePagoCompensaCompraEntradasYCupoTrasCommit() {
        TandaFila tanda = prepararFila(2, 2, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-rechazada", request());
        CompraService servicioConRechazo = servicioConResultado("RECHAZA");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        PagoCompraResponse resultado = transaction.execute(status -> servicioConRechazo.pagar(
                creada.idCompra(), 10L, "pago-rechazado", new PagoCompraRequest("TARJETA")
        ));

        assertEquals(Compra.RECHAZADA, resultado.estado());
        entityManager.clear();
        assertEquals(Compra.RECHAZADA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("RECHAZADO", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                .stream().allMatch(entrada -> "CANCELADA".equals(entrada.getEstado())));
        assertEquals(2, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void devolucionCompletaSeResuelveYReintentaConLaMismaClave() {
        prepararFila(4, 4, 10L);
        CompraRequest dosEntradas = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null)
        ));
        CompraResponse creada = compraService.crear(10L, "compra-devolucion", dosEntradas);
        compraService.pagar(creada.idCompra(), 10L, "pago-devolucion", new PagoCompraRequest("TARJETA"));
        compraService.solicitarDevolucion(creada.idCompra(), 10L);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status ->
                servicioConResultado("FALLA").resolverDevolucion(creada.idCompra(), true));
        entityManager.clear();
        assertEquals("REINTEGRO_PENDIENTE", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        String claveReintegro = compraRepository.findById(creada.idCompra()).orElseThrow().getClaveReintegro();

        transaction.executeWithoutResult(status ->
                servicioConResultado("APRUEBA").reintentarDevolucion(creada.idCompra()));
        transaction.executeWithoutResult(status ->
                servicioConResultado("APRUEBA").reintentarDevolucion(creada.idCompra()));
        entityManager.clear();

        Compra devuelta = compraRepository.findById(creada.idCompra()).orElseThrow();
        assertEquals(Compra.DEVUELTA, devuelta.getEstado());
        assertEquals(claveReintegro, devuelta.getClaveReintegro());
        assertEquals("REEMBOLSADO", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                .stream().allMatch(entrada -> "DEVUELTA".equals(entrada.getEstado())));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void expiracionConcurrenteConPagoNoGeneraCargoTardio() throws Exception {
        prepararFila(2, 2, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-expirable", request());
        Compra compra = compraRepository.findById(creada.idCompra()).orElseThrow();
        compra.setVenceEn(Instant.now().minusSeconds(1));
        compraRepository.saveAndFlush(compra);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> expiracion = executor.submit(() -> {
                await(inicio);
                compraService.expirarComprasVencidas(20L, Instant.now());
            });
            Future<String> pago = executor.submit(() -> {
                await(inicio);
                try {
                    return compraService.pagar(creada.idCompra(), 10L, "pago-tardio",
                            new PagoCompraRequest("TARJETA")).estado();
                } catch (ResponseStatusException esperado) {
                    if (esperado.getStatusCode() == HttpStatus.CONFLICT) {
                        return "CONFLICT";
                    }
                    throw esperado;
                }
            });
            inicio.countDown();
            expiracion.get();
            String resultadoPago = pago.get();
            assertTrue(Compra.VENCIDA.equals(resultadoPago) || "CONFLICT".equals(resultadoPago));

            entityManager.clear();
            assertEquals(Compra.VENCIDA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
            assertNull(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElse(null));
            assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                    .stream().allMatch(entrada -> "CANCELADA".equals(entrada.getEstado())));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierreDeTandaConcurrenteConCreacionNoDejaCompraEnTandaCerrada() throws Exception {
        TandaFila tanda = prepararFila(2, 2, 10L);
        tanda.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(tanda);
        IngresoFila ingreso = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, 10L, "HABILITADO")
                .orElseThrow();
        ingreso.setVenceEn(Instant.now().minusSeconds(1));
        ingresoFilaRepository.saveAndFlush(ingreso);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cierre = executor.submit(() -> {
                await(inicio);
                filaVirtualService.cerrarTandaVencida(20L);
            });
            Future<Boolean> compraRechazada = executor.submit(() -> {
                await(inicio);
                try {
                    compraService.crear(10L, "compra-cierre-concurrente", request());
                    return false;
                } catch (ResponseStatusException esperado) {
                    return esperado.getStatusCode() == HttpStatus.CONFLICT;
                }
            });
            inicio.countDown();
            cierre.get();
            assertTrue(compraRechazada.get());

            entityManager.clear();
            assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
            assertEquals(0, compraRepository.count());
            assertEquals(0, entradaRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelacionConcurrenteConPagoNoDejaCompraPagadaSinReintegro() throws Exception {
        prepararFila(2, 2, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-cancelacion-concurrente", request());
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancelacion = executor.submit(() -> {
                await(inicio);
                filaVirtualService.cancelarEvento(20L);
            });
            Future<?> pago = executor.submit(() -> {
                await(inicio);
                try {
                    compraService.pagar(creada.idCompra(), 10L, "pago-cancelacion-concurrente",
                            new PagoCompraRequest("TARJETA"));
                } catch (ResponseStatusException esperado) {
                    if (esperado.getStatusCode() != HttpStatus.CONFLICT) {
                        throw esperado;
                    }
                }
            });
            inicio.countDown();
            cancelacion.get();
            pago.get();

            entityManager.clear();
            Compra compraFinal = compraRepository.findById(creada.idCompra()).orElseThrow();
            assertTrue(Compra.CANCELADA.equals(compraFinal.getEstado())
                    || Compra.DEVUELTA.equals(compraFinal.getEstado()));
            if (Compra.DEVUELTA.equals(compraFinal.getEstado())) {
                assertEquals("REEMBOLSADO", pagoRepository
                        .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
                assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                        .stream().allMatch(entrada -> "DEVUELTA".equals(entrada.getEstado())));
            } else {
                assertNull(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElse(null));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void compraMultientradaPagadaSeReintegraUnaVezAlCancelarAunqueUnaEntradaEsteUsada() {
        prepararFila(4, 4, 10L);
        CompraRequest dosEntradas = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null)
        ));
        CompraResponse creada = compraService.crear(10L, "compra-cancelar", dosEntradas);
        compraService.pagar(creada.idCompra(), 10L, "pago-compra", new PagoCompraRequest("TARJETA"));
        List<com.ticketya.entrada.entity.Entrada> entradas =
                entradaRepository.findByIdCompraOrderById(creada.idCompra());
        entradas.getFirst().setEstado("USADA");
        entradaRepository.saveAndFlush(entradas.getFirst());

        compraService.cancelarComprasPendientesEvento(20L);
        String referenciaReintegro = pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra())
                .orElseThrow().getReferenciaPasarela();
        compraService.cancelarComprasPendientesEvento(20L);

        entityManager.clear();
        assertEquals(Compra.DEVUELTA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("REEMBOLSADO", pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra())
                .orElseThrow().getEstado());
        assertEquals(referenciaReintegro, pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra())
                .orElseThrow().getReferenciaPasarela());
        assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                .stream().allMatch(entrada -> "DEVUELTA".equals(entrada.getEstado())));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rechazoPorCupoAgotadoCierraTandaYDevuelveHabilitadosAEsperaConSuSecuencia() {
        TandaFila tanda = prepararFila(10, 1, 10L, 11L);
        tanda.setCupoRestante(0);
        tandaFilaRepository.saveAndFlush(tanda);
        FilaEvento fila = filaEventoRepository.findById(20L).orElseThrow();
        fila.setEstado("PAUSADA");
        filaEventoRepository.saveAndFlush(fila);

        CompraResponse rechazo = compraService.crear(10L, "clave-cupo-agotado", request());

        assertNull(rechazo.idCompra());
        assertEquals("CUPO_TANDA_INSUFICIENTE", rechazo.codigo());
        entityManager.clear();
        assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
        assertEquals(0, compraRepository.count());
        assertEquals(1, intentoCompraRepository.count());
        List<IngresoFila> ingresos = ingresoFilaRepository
                .findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(20L, tanda.getId(), "EN_ESPERA");
        assertEquals(2, ingresos.size());
        assertEquals(List.of(1L, 2L), ingresos.stream().map(IngresoFila::getSecuencia).toList());
        assertTrue(ingresos.stream().allMatch(ingreso -> ingreso.getVenceEn() == null));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void vencimientoDescubiertoAlPagarCierraLaTandaSinDependerDelScheduler() {
        TandaFila tanda = prepararFila(2, 1, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-vence-al-pagar", request());
        Compra compra = compraRepository.findById(creada.idCompra()).orElseThrow();
        compra.setVenceEn(Instant.now().minusSeconds(1));
        compraRepository.saveAndFlush(compra);
        IngresoFila ingreso = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, 10L, "HABILITADO")
                .orElseThrow();
        ingreso.setEstado("VENCIDO");
        ingresoFilaRepository.saveAndFlush(ingreso);

        PagoCompraResponse resultado = compraService.pagar(
                creada.idCompra(), 10L, "pago-tardio-descubierto", new PagoCompraRequest("TARJETA"));

        assertEquals(Compra.VENCIDA, resultado.estado());
        entityManager.clear();
        assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
        assertNull(pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElse(null));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void claveDeCompraReutilizadaConPayloadDistintoResponde422() {
        prepararFila(4, 4, 10L);
        compraService.crear(10L, "clave-idempotente", request());
        CompraRequest otroPayload = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null)
        ));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> compraService.crear(10L, "clave-idempotente", otroPayload));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        entityManager.clear();
        assertEquals(1, compraRepository.count());
        assertEquals(1, entradaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void compraVencidaSeCompensaAntesDeCrearLaSiguiente() {
        TandaFila tanda = prepararFila(4, 4, 10L);
        CompraResponse vencida = compraService.crear(10L, "compra-vencida-inline", request());
        Compra entidadVencida = compraRepository.findById(vencida.idCompra()).orElseThrow();
        entidadVencida.setVenceEn(Instant.now().minusSeconds(1));
        compraRepository.saveAndFlush(entidadVencida);

        CompraResponse nueva = compraService.crear(10L, "compra-tras-vencimiento", request());

        assertNotNull(nueva.idCompra());
        assertNotEquals(vencida.idCompra(), nueva.idCompra());
        entityManager.clear();
        assertEquals(Compra.VENCIDA,
                compraRepository.findById(vencida.idCompra()).orElseThrow().getEstado());
        assertEquals(Compra.RESERVADA,
                compraRepository.findById(nueva.idCompra()).orElseThrow().getEstado());
        assertEquals(3, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
        assertEquals(2, compraRepository.count());
        assertEquals(2, entradaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void pagoDeSeisEntradasGeneraUnQrYUnRegistroPorEntrada() {
        prepararFila(10, 6, 10L);
        CompraRequest seisEntradas = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null)
        ));
        CompraResponse creada = compraService.crear(10L, "compra-seis-qr", seisEntradas);

        PagoCompraResponse pago = compraService.pagar(
                creada.idCompra(), 10L, "pago-seis-qr", new PagoCompraRequest("TARJETA")
        );

        assertEquals(Compra.PAGADA, pago.estado());
        assertEquals(new BigDecimal("150.00"), pago.total());
        entityManager.clear();
        List<Entrada> entradas = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        assertEquals(6, entradas.size());
        assertTrue(entradas.stream().allMatch(entrada ->
                "VALIDA".equals(entrada.getEstado())
                        && entrada.getCodigoQr() != null
                        && !entrada.getCodigoQr().isBlank()));
        assertEquals(6, entradas.stream().map(Entrada::getCodigoQr).distinct().count());
        assertEquals(6, registroPagoRepository.count());
        assertEquals(6, registroPagoRepository.findByUsuario(10L).size());
        assertEquals(1, pagoRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void compraSinTurnoVigenteResponde409() {
        prepararFila(4, 4, 99L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> compraService.crear(10L, "sin-turno", request()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        entityManager.clear();
        assertEquals(0, compraRepository.count());
        assertEquals(0, entradaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ventaLibreTrasCierreManualAsignaDiezMinutosDesdeLaCreacion() {
        prepararFilaCerradaManualmente();
        Instant antes = Instant.now();

        CompraResponse creada = compraService.crear(10L, "venta-libre", request());

        assertNotNull(creada.venceEn());
        assertTrue(!creada.venceEn().isBefore(antes.plus(9, java.time.temporal.ChronoUnit.MINUTES)));
        assertTrue(!creada.venceEn().isAfter(Instant.now().plus(10, java.time.temporal.ChronoUnit.MINUTES)));
        entityManager.clear();
        assertEquals(1, compraRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ventaLibreTrasCierreManualRequiereEventoPublicado() {
        prepararFilaCerradaManualmente();
        when(eventoClient.obtenerPorId(20L)).thenReturn(new EventoResponse(
                20L, null, 90L, "Evento", null, Instant.now().plusSeconds(345_600), "BORRADOR",
                null, List.of(), List.of(new EventoSectorResponse(30L, new BigDecimal("25.00")))
        ));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> compraService.crear(10L, "venta-libre-no-publicado", request()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        entityManager.clear();
        assertEquals(0, compraRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void unaSolaCompraReservadaComproTerminalYReingresoConNuevaSecuencia() {
        TandaFila tanda = prepararFila(4, 4, 10L);
        CompraResponse primera = compraService.crear(10L, "primera-reservada", request());

        CompraResponse segunda = compraService.crear(10L, "segunda-reservada", request());
        assertEquals("COMPRA_PENDIENTE", segunda.codigo());
        assertNull(segunda.idCompra());

        compraService.pagar(primera.idCompra(), 10L, "pago-unica-reservada", new PagoCompraRequest("TARJETA"));
        entityManager.clear();
        IngresoFila consumido = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, 10L, "COMPRO")
                .orElseThrow();
        assertEquals(tanda.getId(), consumido.getIdTanda());

        var reingreso = filaVirtualService.unirse(20L, 10L);
        assertNotEquals(consumido.getId(), reingreso.idIngreso());
        assertNotEquals(consumido.getSecuencia(), reingreso.secuencia());
        assertEquals(2L, reingreso.secuencia());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierrePorTiempoExpulsaSoloAQuienNoComproNiRegistroRechazo() {
        TandaFila tanda = prepararFila(6, 6, 10L, 11L, 12L);
        pausarFila();
        CompraResponse comprada = compraService.crear(10L, "compra-antes-del-vencimiento", request());
        Compra entidad = compraRepository.findById(comprada.idCompra()).orElseThrow();
        entidad.setVenceEn(Instant.now().minusSeconds(1));
        compraRepository.saveAndFlush(entidad);
        IntentoCompra rechazo = new IntentoCompra();
        rechazo.setIdUsuario(11L);
        rechazo.setIdEvento(20L);
        rechazo.setIdTanda(tanda.getId());
        rechazo.setCreadoEn(Instant.now());
        rechazo.setResultado("CUPO_TANDA_INSUFICIENTE");
        intentoCompraRepository.saveAndFlush(rechazo);
        TandaFila vencida = tandaFilaRepository.findById(tanda.getId()).orElseThrow();
        vencida.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(vencida);

        filaVirtualService.cerrarTandaVencida(20L);

        entityManager.clear();
        assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
        assertEquals(Compra.VENCIDA,
                compraRepository.findById(comprada.idCompra()).orElseThrow().getEstado());
        assertEquals("VENCIDO", estadoDe(10L));
        assertEquals("EN_ESPERA", estadoDe(11L));
        assertEquals("VENCIDO", estadoDe(12L));
        Entrada compensada = entradaRepository.findByIdCompraOrderById(comprada.idCompra()).getFirst();
        assertEquals("CANCELADA", compensada.getEstado());
        assertNull(compensada.getCodigoQr());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierrePorCupoDevuelveAlHabilitadoSinRechazoConservandoSecuencia() {
        TandaFila tanda = prepararFila(2, 1, 10L, 11L, 12L);
        pausarFila();
        CompraResponse comprada = compraService.crear(10L, "compra-ultimo-cupo", request());
        CompraResponse rechazada = compraService.crear(11L, "rechazo-ultimo-cupo", request());
        assertEquals("CUPO_TANDA_INSUFICIENTE", rechazada.codigo());

        compraService.pagar(comprada.idCompra(), 10L, "pago-tras-cupo", new PagoCompraRequest("TARJETA"));

        entityManager.clear();
        assertEquals(1, intentoCompraRepository.count());
        assertEquals("EN_ESPERA", estadoDe(11L));
        assertEquals("EN_ESPERA", estadoDe(12L));
        assertEquals(2L, ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 11L).orElseThrow().getSecuencia());
        assertEquals(3L, ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, 12L).orElseThrow().getSecuencia());
        assertEquals("COMPRO", estadoDe(10L));
        assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
        assertEquals(0, tandaFilaRepository.findAll().stream()
                .filter(abierta -> "ABIERTA".equals(abierta.getEstado())).count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierreAnticipadoPorResolucionCompletaSinComprasPendientes() {
        TandaFila tanda = prepararFila(4, 4, 10L, 11L);
        CompraResponse primera = compraService.crear(10L, "resuelta-10", request());
        CompraResponse segunda = compraService.crear(11L, "resuelta-11", request());

        compraService.pagar(primera.idCompra(), 10L, "pago-resuelta-10", new PagoCompraRequest("TARJETA"));
        compraService.pagar(segunda.idCompra(), 11L, "pago-resuelta-11", new PagoCompraRequest("TARJETA"));

        entityManager.clear();
        assertEquals("CERRADA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
        assertEquals("COMPRO", estadoDe(10L));
        assertEquals("COMPRO", estadoDe(11L));
        assertEquals(1, tandaFilaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierreVencidoNoAbreTandaSiLaFilaEstaPausada() {
        TandaFila vencida = prepararFila(4, 2, 10L);
        filaVirtualService.unirse(20L, 11L);
        pausarFila();
        vencida.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(vencida);
        entityManager.clear();

        filaVirtualService.cerrarTandaVencida(20L);

        entityManager.clear();
        assertEquals(1, tandaFilaRepository.count());
        assertEquals("CERRADA", tandaFilaRepository.findById(vencida.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ticksDuplicadosNoAbrenDosTandas() {
        TandaFila vencida = prepararFila(4, 2, 10L);
        filaVirtualService.unirse(20L, 11L);
        vencida.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(vencida);
        entityManager.clear();

        filaVirtualService.cerrarTandaVencida(20L);
        filaVirtualService.cerrarTandaVencida(20L);

        entityManager.clear();
        assertEquals(2, tandaFilaRepository.count());
        assertEquals(1, tandaFilaRepository.findAll().stream()
                .filter(abierta -> "ABIERTA".equals(abierta.getEstado())).count());
        assertEquals("HABILITADO", estadoDe(11L));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierreVencidoSinPersonasEsperandoNoAbreTandaVacia() {
        TandaFila vencida = prepararFila(4, 2, 10L);
        vencida.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(vencida);
        entityManager.clear();

        filaVirtualService.cerrarTandaVencida(20L);

        entityManager.clear();
        assertEquals(1, tandaFilaRepository.count());
        assertEquals("CERRADA", tandaFilaRepository.findById(vencida.getId()).orElseThrow().getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flagDeshabilitadoImpideActivarIngresarYCrearCompra() {
        prepararFila(4, 4, 10L);
        FilaVirtualProperties deshabilitado = new FilaVirtualProperties();
        deshabilitado.setEnabled(false);
        compraService.setFilaVirtualProperties(deshabilitado);
        filaVirtualService.setFilaVirtualProperties(deshabilitado);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> filaVirtualService.activar(20L, 10L, null)).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> filaVirtualService.unirse(20L, 12L)).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> compraService.crear(11L, "bloqueada-por-flag", request())).getStatusCode());

        entityManager.clear();
        assertEquals(0, compraRepository.count());
        assertEquals(0, entradaRepository.count());
        assertEquals(1, ingresoFilaRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flagDeshabilitadoPermitePagarYDevolverCompraExistente() {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-drenaje", request());
        compraService.pagar(creada.idCompra(), 10L, "pago-drenaje", new PagoCompraRequest("TARJETA"));
        deshabilitarFila();

        compraService.solicitarDevolucion(creada.idCompra(), 10L);
        compraService.resolverDevolucion(creada.idCompra(), true);

        entityManager.clear();
        assertEquals(Compra.DEVUELTA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flagDeshabilitadoPermiteExpirarCompraReservada() {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-expirar-flag", request());
        Instant vencida = compraRepository.findById(creada.idCompra()).orElseThrow().getVenceEn().plusSeconds(1);
        deshabilitarFila();

        compraService.expirarComprasVencidas(20L, vencida);

        entityManager.clear();
        assertEquals(Compra.VENCIDA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        Entrada compensada = entradaRepository.findByIdCompraOrderById(creada.idCompra()).getFirst();
        assertEquals("CANCELADA", compensada.getEstado());
        assertNull(compensada.getCodigoQr());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flagDeshabilitadoPermiteCancelarEventoConCompraPendiente() {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-cancelar-flag", request());
        deshabilitarFila();

        filaVirtualService.cancelarEvento(20L);

        entityManager.clear();
        assertEquals(Compra.CANCELADA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("CERRADA", filaEventoRepository.findById(20L).orElseThrow().getEstado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackConFlagDeshabilitadoDrenaComprasYConservaDatosLegacy() {
        TandaFila tanda = prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-rollback", request());
        Long idEntradaLegacy = jdbcTemplate.queryForObject("""
                INSERT INTO entradas (id_evento, id_usuario, id_sector, estado, precio, expira_en, clave_idempotencia)
                VALUES (21, 99, 30, 'RESERVADA', 25.00, CURRENT_TIMESTAMP + INTERVAL '10 minutes',
                        'entrada-legacy-rollback')
                RETURNING id
                """, Long.class);
        Instant vencida = compraRepository.findById(creada.idCompra()).orElseThrow().getVenceEn().plusSeconds(1);
        deshabilitarFila();

        compraService.expirarComprasVencidas(20L, vencida);
        PagoRequest pagoLegacy = new PagoRequest();
        pagoLegacy.setIdEntrada(idEntradaLegacy);
        pagoLegacy.setIdUsuario(99L);
        pagoLegacy.setMedioDePago("TARJETA");
        pagoLegacy.setClaveIdempotencia("pago-legacy-rollback");
        pagoService.crear(pagoLegacy);

        entityManager.clear();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> compraService.crear(11L, "bloqueada-tras-rollback", request())).getStatusCode());
        assertEquals(Compra.VENCIDA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertTrue(filaEventoRepository.findById(20L).isPresent());
        assertTrue(tandaFilaRepository.findById(tanda.getId()).isPresent());
        assertEquals(4, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
        Entrada legacy = entradaRepository.findById(idEntradaLegacy).orElseThrow();
        assertEquals("VALIDA", legacy.getEstado());
        assertNull(legacy.getIdCompra());
        assertEquals(1, registroPagoRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flagDeshabilitadoNoActivaFallbackLegacyParaEventoConFila() {
        prepararFilaCerradaManualmente();
        deshabilitarFila();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> compraService.crear(10L, "fallback-bloqueado", request())).getStatusCode());
        assertEquals(0, compraRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cierreConcurrenteConCreacionDejaCompraSoloEnTandaAbiertaOCupoEntero() throws Exception {
        for (int intento = 0; intento < 12; intento++) {
            limpiarDatos();
            TandaFila tanda = prepararFila(4, 4, 10L);
            tanda.setVenceEn(Instant.now().minusSeconds(1));
            tandaFilaRepository.saveAndFlush(tanda);
            entityManager.clear();
            CountDownLatch inicio = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> cierre = executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.cerrarTandaVencida(20L);
                });
                Future<CompraResponse> compra = executor.submit(() -> crearAlMismoTiempo(inicio, 10L, "cierre-concurrente"));

                inicio.countDown();
                CompraResponse resultado = null;
                ResponseStatusException conflicto = null;
                try {
                    resultado = compra.get();
                } catch (ExecutionException excepcion) {
                    if (excepcion.getCause() instanceof ResponseStatusException responseException
                            && responseException.getStatusCode() == HttpStatus.CONFLICT) {
                        conflicto = responseException;
                    } else {
                        throw excepcion;
                    }
                }
                cierre.get();

                entityManager.clear();
                TandaFila finalTanda = tandaFilaRepository.findById(tanda.getId()).orElseThrow();
                assertEquals("CERRADA", finalTanda.getEstado());
                if (conflicto != null) {
                    assertEquals(0, compraRepository.count());
                    assertEquals(0, entradaRepository.count());
                    assertEquals(4, finalTanda.getCupoRestante());
                } else {
                    assertNotNull(resultado.idCompra());
                    assertEquals(1, compraRepository.count());
                    assertEquals(tanda.getId(), compraRepository.findAll().getFirst().getIdTanda());
                    assertEquals(1, entradaRepository.count());
                    assertEquals(3, finalTanda.getCupoRestante());
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void nConcurrentesSobreCapacidadNCompranTodosSinRechazo() throws Exception {
        int n = 8;
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 2 * n, null, null, "NO_NUMERADO")
        );
        TandaFila tanda = prepararFilaAbiertaVacia(2 * n, 2 * n, n);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(n);
        try {
            List<Future<CompraResponse>> resultados = new ArrayList<>();
            for (long id = 10L; id < 10L + n; id++) {
                long usuario = id;
                resultados.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                    CompraResponse compra = compraService.crear(
                            usuario, "conc-" + usuario, requestDosEntradas()
                    );
                    if (compra.idCompra() != null) {
                        compraService.pagar(compra.idCompra(), usuario, "pago-" + usuario,
                                new PagoCompraRequest("TARJETA"));
                    }
                    return compra;
                }));
            }
            inicio.countDown();
            for (Future<CompraResponse> futuro : resultados) {
                CompraResponse compra = futuro.get();
                assertNotNull(compra.idCompra(), "todos deben obtener turno: " + compra.codigo());
                assertNull(compra.codigo());
            }
            entityManager.clear();
            assertEquals(n, compraRepository.count());
            assertEquals(2 * n, entradaRepository.count());
            assertEquals(n, compraRepository.findAll().stream()
                    .filter(compra -> "PAGADA".equals(compra.getEstado())).count());
            assertEquals(0, intentoCompraRepository.count());
            assertEquals(0, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void doceIngresosConcurrentesRespetanTopeMaxPersonas() throws Exception {
        prepararFilaAbiertaVacia(24, 24, 8);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            List<Future<?>> resultados = new ArrayList<>();
            for (long id = 30L; id < 42L; id++) {
                long usuario = id;
                resultados.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                }));
            }
            inicio.countDown();
            for (Future<?> resultado : resultados) {
                resultado.get();
            }
            entityManager.clear();
            List<IngresoFila> habilitados = ingresoFilaRepository
                    .findByIdEventoAndEstadoOrderBySecuenciaAsc(20L, "HABILITADO");
            List<IngresoFila> esperando = ingresoFilaRepository
                    .findByIdEventoAndEstadoOrderBySecuenciaAsc(20L, "EN_ESPERA");
            assertEquals(8, habilitados.size());
            assertEquals(4, esperando.size());
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L),
                    habilitados.stream().map(IngresoFila::getSecuencia).sorted().toList());
            assertEquals(List.of(9L, 10L, 11L, 12L),
                    esperando.stream().map(IngresoFila::getSecuencia).sorted().toList());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void loteNoSuperaMaxPersonasCuandoDiezYaCompraronEnLaMismaTanda() throws Exception {
        int maxPersonas = 30;
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 120, null, null, "NO_NUMERADO")
        );
        TandaFila tanda = prepararFilaAbiertaVacia(120, 120, maxPersonas);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(maxPersonas);
        try {
            List<Future<?>> ingresos = new ArrayList<>();
            for (long id = 100L; id < 100L + maxPersonas; id++) {
                long usuario = id;
                ingresos.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                }));
            }
            inicio.countDown();
            for (Future<?> ingreso : ingresos) {
                ingreso.get();
            }
            entityManager.clear();
            assertEquals(30, ingresoFilaRepository.buscarHabilitadosDeTanda(20L, tanda.getId()).size());

            for (long id = 100L; id < 110L; id++) {
                CompraResponse compra = compraService.crear(id, "lote-" + id, requestDosEntradas());
                assertNotNull(compra.idCompra(), "la compra debía reservarse: " + compra.codigo());
                compraService.pagar(compra.idCompra(), id, "pago-lote-" + id,
                        new PagoCompraRequest("TARJETA"));
            }
            entityManager.clear();
            assertEquals(10, ingresoFilaRepository
                    .findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(20L, tanda.getId(), "COMPRO").size());
            assertEquals(20, ingresoFilaRepository.buscarHabilitadosDeTanda(20L, tanda.getId()).size());
            assertEquals("ABIERTA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
            assertTrue(tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante() > 0);

            CountDownLatch segundaEntrada = new CountDownLatch(1);
            ExecutorService segundoExecutor = Executors.newFixedThreadPool(5);
            try {
                List<Future<?>> nuevos = new ArrayList<>();
                for (long id = 200L; id < 205L; id++) {
                    long usuario = id;
                    nuevos.add(segundoExecutor.submit(() -> {
                        await(segundaEntrada);
                        filaVirtualService.unirse(20L, usuario);
                    }));
                }
                segundaEntrada.countDown();
                for (Future<?> nuevo : nuevos) {
                    nuevo.get();
                }
            } finally {
                segundoExecutor.shutdownNow();
            }
            entityManager.clear();
            Integer asignados = jdbcTemplate.queryForObject(
                    "select count(*) from entrada_fila where id_evento = 20 and id_tanda = ?",
                    Integer.class, tanda.getId());
            assertEquals(30, asignados, "el lote de la tanda 1 no debe superar maxPersonas=30");
            for (long id = 200L; id < 205L; id++) {
                assertEquals("EN_ESPERA", estadoDe(id), "el recién llegado " + id + " no debe entrar en la tanda llena");
            }
            assertEquals(1, tandaFilaRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void loteCuentaCompradoresYTurnosVencidosSinSuperarMaxPersonas() throws Exception {
        int maxPersonas = 30;
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 120, null, null, "NO_NUMERADO")
        );
        TandaFila tanda = prepararFilaAbiertaVacia(120, 120, maxPersonas);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(maxPersonas);
        try {
            List<Future<?>> ingresos = new ArrayList<>();
            for (long id = 300L; id < 300L + maxPersonas; id++) {
                long usuario = id;
                ingresos.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                }));
            }
            inicio.countDown();
            for (Future<?> ingreso : ingresos) {
                ingreso.get();
            }
            entityManager.clear();
            assertEquals(30, ingresoFilaRepository.contarAsignadosDeTanda(20L, tanda.getId()));

            for (long id = 300L; id < 310L; id++) {
                CompraResponse compra = compraService.crear(id, "lote-mixto-" + id, requestDosEntradas());
                assertNotNull(compra.idCompra(), "la compra debía reservarse: " + compra.codigo());
                compraService.pagar(compra.idCompra(), id, "pago-mixto-" + id,
                        new PagoCompraRequest("TARJETA"));
            }
            for (long id = 310L; id < 315L; id++) {
                IngresoFila vencido = ingresoFilaRepository
                        .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, id, "HABILITADO")
                        .orElseThrow();
                vencido.setEstado("VENCIDO");
                vencido.setVenceEn(null);
                ingresoFilaRepository.saveAndFlush(vencido);
            }
            entityManager.clear();
            assertEquals(10, ingresoFilaRepository
                    .findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(20L, tanda.getId(), "COMPRO").size());
            assertEquals(5, ingresoFilaRepository
                    .findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(20L, tanda.getId(), "VENCIDO").size());
            assertEquals(15, ingresoFilaRepository.buscarHabilitadosDeTanda(20L, tanda.getId()).size());
            assertEquals(30, ingresoFilaRepository.contarAsignadosDeTanda(20L, tanda.getId()));
            assertTrue(ingresoFilaRepository.findByIdEventoAndIdTandaAndEstadoOrderBySecuenciaAsc(
                    20L, tanda.getId(), "VENCIDO").stream().allMatch(i -> tanda.getId().equals(i.getIdTanda())));
            assertEquals("ABIERTA", tandaFilaRepository.findById(tanda.getId()).orElseThrow().getEstado());
            assertTrue(tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante() > 0);

            CountDownLatch segundaEntrada = new CountDownLatch(1);
            ExecutorService segundoExecutor = Executors.newFixedThreadPool(5);
            try {
                List<Future<?>> nuevos = new ArrayList<>();
                for (long id = 400L; id < 405L; id++) {
                    long usuario = id;
                    nuevos.add(segundoExecutor.submit(() -> {
                        await(segundaEntrada);
                        filaVirtualService.unirse(20L, usuario);
                    }));
                }
                segundaEntrada.countDown();
                for (Future<?> nuevo : nuevos) {
                    nuevo.get();
                }
            } finally {
                segundoExecutor.shutdownNow();
            }
            entityManager.clear();
            assertEquals(30, ingresoFilaRepository.contarAsignadosDeTanda(20L, tanda.getId()),
                    "el lote de la tanda 1 no debe superar maxPersonas=30");
            for (long id = 400L; id < 405L; id++) {
                assertEquals("EN_ESPERA", estadoDe(id), "el recién llegado " + id + " no debe entrar en la tanda llena");
            }
            assertEquals(1, tandaFilaRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void habilitacionRespetaOrdenDeSecuenciaYNoAdelantaAQuienEsperaba() throws Exception {
        TandaFila primera = prepararFilaAbiertaVacia(10, 10, 2);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        TandaFila segunda;
        Long idRezagado;
        try {
            List<Future<?>> resultados = new ArrayList<>();
            for (long id = 50L; id < 53L; id++) {
                long usuario = id;
                resultados.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                }));
            }
            inicio.countDown();
            for (Future<?> resultado : resultados) {
                resultado.get();
            }
            entityManager.clear();
            List<IngresoFila> habilitados = ingresoFilaRepository
                    .findByIdEventoAndEstadoOrderBySecuenciaAsc(20L, "HABILITADO");
            List<IngresoFila> esperando = ingresoFilaRepository
                    .findByIdEventoAndEstadoOrderBySecuenciaAsc(20L, "EN_ESPERA");
            assertEquals(List.of(1L, 2L), habilitados.stream()
                    .map(IngresoFila::getSecuencia).sorted().toList());
            assertEquals(List.of(3L), esperando.stream()
                    .map(IngresoFila::getSecuencia).sorted().toList());
            assertTrue(habilitados.stream().allMatch(i -> primera.getId().equals(i.getIdTanda())));
            idRezagado = esperando.getFirst().getIdUsuario();
            assertTrue(idRezagado >= 50L && idRezagado <= 52L,
                    "el rezagado debe ser uno de los usuarios creados por el test");
        } finally {
            executor.shutdownNow();
        }

        entityManager.clear();
        primera.setVenceEn(Instant.now().minusSeconds(1));
        tandaFilaRepository.saveAndFlush(primera);
        entityManager.clear();
        filaVirtualService.cerrarTandaVencida(20L);
        entityManager.clear();
        segunda = tandaFilaRepository.findFirstByIdEventoOrderByNumeroDesc(20L).orElseThrow();
        assertNotEquals(primera.getId(), segunda.getId());
        IngresoFila rezagado = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, idRezagado).orElseThrow();
        assertEquals("HABILITADO", rezagado.getEstado());
        assertEquals(3L, rezagado.getSecuencia());
        assertEquals(segunda.getId(), rezagado.getIdTanda());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void demandaMayorAlCupoVendeExactamenteCupoEntradas() throws Exception {
        int cupo = 4;
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 10, null, null, "NO_NUMERADO")
        );
        TandaFila tanda = prepararFilaAbiertaVacia(10, cupo, 6);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<CompraResponse>> resultados = new ArrayList<>();
            for (long id = 70L; id < 76L; id++) {
                long usuario = id;
                resultados.add(executor.submit(() -> {
                    await(inicio);
                    filaVirtualService.unirse(20L, usuario);
                    return compraService.crear(usuario, "cupo-" + usuario, requestDosEntradas());
                }));
            }
            inicio.countDown();
            List<CompraResponse> respuestas = new ArrayList<>();
            for (Future<CompraResponse> futuro : resultados) {
                respuestas.add(futuro.get());
            }
            assertEquals(2, respuestas.stream().filter(r -> r.idCompra() != null).count());
            assertEquals(4, respuestas.stream()
                    .filter(r -> "CUPO_TANDA_INSUFICIENTE".equals(r.codigo())).count());
            entityManager.clear();
            assertEquals(4, entradaRepository.count());
            assertEquals(2, compraRepository.count());
            assertEquals(4, intentoCompraRepository.count());
            assertEquals(0, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());
        } finally {
            executor.shutdownNow();
        }
    }

    private TandaFila prepararFilaAbiertaVacia(int capacidad, int cupo, int maxPersonas) {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");
        fila.setMaxPersonas(maxPersonas);
        fila.setCupoEntradas(cupo);
        fila.setCapacidadTotal(capacidad);
        fila.setSiguienteSecuencia(1L);
        filaEventoRepository.saveAndFlush(fila);

        TandaFila tanda = new TandaFila();
        tanda.setIdEvento(20L);
        tanda.setNumero(1L);
        tanda.setEstado("ABIERTA");
        tanda.setIniciadaEn(Instant.now());
        tanda.setVenceEn(Instant.now().plusSeconds(600));
        tanda.setCupoEfectivo(cupo);
        tanda.setCupoRestante(cupo);
        return tandaFilaRepository.saveAndFlush(tanda);
    }

    private CompraRequest requestDosEntradas() {
        return new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null),
                new EntradaCompraRequest(30L, null, null)
        ));
    }

    private void deshabilitarFila() {
        FilaVirtualProperties deshabilitado = new FilaVirtualProperties();
        deshabilitado.setEnabled(false);
        compraService.setFilaVirtualProperties(deshabilitado);
        filaVirtualService.setFilaVirtualProperties(deshabilitado);
    }

    private void limpiarDatos() {
        jdbcTemplate.execute("DELETE FROM registros_pago");
        jdbcTemplate.execute("DELETE FROM pagos");
        jdbcTemplate.execute("DELETE FROM entradas");
        jdbcTemplate.execute("DELETE FROM intento_compra");
        jdbcTemplate.execute("DELETE FROM compras");
        jdbcTemplate.execute("DELETE FROM entrada_fila");
        jdbcTemplate.execute("DELETE FROM tanda_fila");
        jdbcTemplate.execute("DELETE FROM fila_evento");
    }

    private void pausarFila() {
        FilaEvento fila = filaEventoRepository.findById(20L).orElseThrow();
        fila.setEstado("PAUSADA");
        filaEventoRepository.saveAndFlush(fila);
    }

    private String estadoDe(Long idUsuario) {
        return ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(20L, idUsuario)
                .orElseThrow().getEstado();
    }

    private void prepararFilaCerradaManualmente() {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("CERRADA");
        fila.setCausaCierre("MANUAL");
        fila.setMaxPersonas(2);
        fila.setCupoEntradas(4);
        fila.setCapacidadTotal(10);
        fila.setSiguienteSecuencia(1L);
        filaEventoRepository.saveAndFlush(fila);
    }

    private CompraResponse crearAlMismoTiempo(CountDownLatch inicio, Long idUsuario, String clave) throws Exception {
        inicio.await();
        return compraService.crear(idUsuario, clave, request());
    }

    private CompraRequest request() {
        return new CompraRequest(20L, List.of(new EntradaCompraRequest(30L, null, null)));
    }

    private CompraService servicioConResultado(String resultado) {
        return new CompraService(
                compraRepository,
                entradaRepository,
                filaEventoRepository,
                ingresoFilaRepository,
                intentoCompraRepository,
                tandaFilaRepository,
                pagoRepository,
                registroPagoRepository,
                eventoClient,
                sectorClient,
                eventPublisher,
                resultado,
                6
        );
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

    private void await(CountDownLatch inicio) {
        try {
            inicio.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void correrMigracion(ApplicationArguments argumentos) {
        try {
            indexMigration.run(argumentos);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
