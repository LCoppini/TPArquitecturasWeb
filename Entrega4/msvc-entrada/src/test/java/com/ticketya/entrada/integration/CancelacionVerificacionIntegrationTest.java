package com.ticketya.entrada.integration;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.config.FilaVirtualIndexMigration;
import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.service.CancelacionEventoService;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.EntradaService;
import com.ticketya.entrada.service.FilaVirtualService;
import com.ticketya.entrada.service.PagoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
@Import({CompraService.class, FilaVirtualService.class, FilaVirtualIndexMigration.class})
@Testcontainers
class CancelacionVerificacionIntegrationTest {

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
    @Autowired private RegistroPagoRepository registroPagoRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EventoClient eventoClient;
    @MockitoBean private SectorClient sectorClient;
    @MockitoBean private UsuarioClient usuarioClient;

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
        jdbcTemplate.execute("DELETE FROM registros_pago");
        jdbcTemplate.execute("DELETE FROM pagos");
        jdbcTemplate.execute("DELETE FROM entradas");
        jdbcTemplate.execute("DELETE FROM intento_compra");
        jdbcTemplate.execute("DELETE FROM compras");
        jdbcTemplate.execute("DELETE FROM entrada_fila");
        jdbcTemplate.execute("DELETE FROM tanda_fila");
        jdbcTemplate.execute("DELETE FROM fila_evento");
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
    void falloDePasarelaCompensaYLiberaElTurnoParaUnNuevoIntento() {
        TandaFila tanda = prepararFila(4, 4, 10L);
        Instant venceEnOriginal = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, 10L, "HABILITADO")
                .orElseThrow().getVenceEn();
        CompraResponse creada = compraService.crear(10L, "compra-fallo-integracion", request());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        PagoCompraResponse resultado = transaction.execute(status -> servicioConResultado("FALLA").pagar(
                creada.idCompra(), 10L, "pago-fallo-integracion", new PagoCompraRequest("TARJETA")
        ));

        assertEquals(Compra.RECHAZADA, resultado.estado());
        entityManager.clear();
        assertEquals(Compra.RECHAZADA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("FALLIDO", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        assertTrue(entradaRepository.findByIdCompraOrderById(creada.idCompra())
                .stream().allMatch(entrada -> "CANCELADA".equals(entrada.getEstado())));
        assertEquals(4, tandaFilaRepository.findById(tanda.getId()).orElseThrow().getCupoRestante());

        CompraResponse nuevoIntento = compraService.crear(10L, "compra-reintento-integracion", request());
        assertTrue(nuevoIntento.idCompra() != null);
        entityManager.clear();
        IngresoFila ingreso = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(20L, 10L, "HABILITADO")
                .orElseThrow();
        assertEquals(venceEnOriginal, ingreso.getVenceEn());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelacionConservaMotivoTrasReintegroFallidoYReintento() {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-cancelar-integracion", request());
        compraService.pagar(creada.idCompra(), 10L, "pago-cancelar-integracion", new PagoCompraRequest("TARJETA"));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        FilaVirtualService filaConFallo = filaConResultado("FALLA");

        transaction.executeWithoutResult(status -> filaConFallo.cancelarEvento(20L));
        entityManager.clear();
        Entrada entradaEnReintegro = entradaRepository.findByIdCompraOrderById(creada.idCompra()).getFirst();
        assertEquals("EN_REINTEGRO", entradaEnReintegro.getEstado());
        assertEquals("CANCELACION", entradaEnReintegro.getMotivoInvalidacion());
        assertEquals("REINTEGRO_PENDIENTE", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        String claveReintegro = compraRepository.findById(creada.idCompra()).orElseThrow().getClaveReintegro();

        transaction.executeWithoutResult(status ->
                servicioConResultado("APRUEBA").reintentarDevolucion(creada.idCompra()));
        entityManager.clear();
        Entrada entradaDevuelta = entradaRepository.findByIdCompraOrderById(creada.idCompra()).getFirst();
        assertEquals("DEVUELTA", entradaDevuelta.getEstado());
        assertEquals("CANCELACION", entradaDevuelta.getMotivoInvalidacion());
        assertEquals("REEMBOLSADO", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        assertEquals(claveReintegro, compraRepository.findById(creada.idCompra()).orElseThrow().getClaveReintegro());
        FilaEvento filaCerrada = filaEventoRepository.findById(20L).orElseThrow();
        assertEquals("CERRADA", filaCerrada.getEstado());
        assertEquals("CANCELACION", filaCerrada.getCausaCierre());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelacionConEntradaUsadaReintegraElTotalYNoDuplicaElReintegro() {
        prepararFila(4, 6, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-cancelar-usada", request(2));
        compraService.pagar(creada.idCompra(), 10L, "pago-cancelar-usada", new PagoCompraRequest("TARJETA"));
        List<Entrada> pagadas = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        assertEquals(2, pagadas.size());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> assertEquals(1, entradaRepository.marcarUsada(
                pagadas.getFirst().getCodigoQr(), 20L, Instant.now(), 101L)));

        transaction.executeWithoutResult(status -> cancelacionConResultado("FALLA").cancelarEvento(20L));
        entityManager.clear();

        List<Entrada> enReintegro = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        assertEquals(2, enReintegro.size());
        assertTrue(enReintegro.stream().allMatch(entrada -> "EN_REINTEGRO".equals(entrada.getEstado())));
        assertTrue(enReintegro.stream().allMatch(entrada -> "CANCELACION".equals(entrada.getMotivoInvalidacion())));
        assertEquals(Compra.PAGADA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("REINTEGRO_PENDIENTE", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        String claveReintegro = compraRepository.findById(creada.idCompra()).orElseThrow().getClaveReintegro();
        assertNotNull(claveReintegro);
        assertEquals(2, registroPagoRepository.count());

        transaction.executeWithoutResult(status ->
                servicioConResultado("APRUEBA").reintentarDevolucion(creada.idCompra()));
        entityManager.clear();

        List<Entrada> devueltas = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        assertTrue(devueltas.stream().allMatch(entrada -> "DEVUELTA".equals(entrada.getEstado())));
        assertTrue(devueltas.stream().allMatch(entrada -> "CANCELACION".equals(entrada.getMotivoInvalidacion())));
        assertEquals(Compra.DEVUELTA, compraRepository.findById(creada.idCompra()).orElseThrow().getEstado());
        assertEquals("REEMBOLSADO", pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getEstado());
        String referencia = pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getReferenciaPasarela();
        assertEquals("refund-" + claveReintegro, referencia);
        assertEquals(1, pagoRepository.count());

        transaction.executeWithoutResult(status ->
                servicioConResultado("APRUEBA").reintentarDevolucion(creada.idCompra()));
        entityManager.clear();
        assertEquals(claveReintegro, compraRepository.findById(creada.idCompra()).orElseThrow().getClaveReintegro());
        assertEquals(referencia, pagoRepository
                .findFirstByIdCompraOrderByIdPagoDesc(creada.idCompra()).orElseThrow().getReferenciaPasarela());
        assertEquals(1, pagoRepository.count());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void pagoYCancelacionConcurrentesDejanUnSoloResultadoConsistente() throws Exception {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-carrera-cancelacion", request());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> pago = executor.submit(() -> {
                inicio.await();
                try {
                    PagoCompraResponse respuesta = transaction.execute(status -> servicioConResultado("APRUEBA").pagar(
                            creada.idCompra(), 10L, "pago-carrera-cancelacion", new PagoCompraRequest("TARJETA")
                    ));
                    return "PAGO_" + respuesta.estado();
                } catch (ResponseStatusException exception) {
                    return "PAGO_" + exception.getStatusCode().value();
                }
            });
            Future<String> cancelacion = executor.submit(() -> {
                inicio.await();
                transaction.executeWithoutResult(status -> cancelacionConResultado("APRUEBA").cancelarEvento(20L));
                return "CANCELACION_OK";
            });
            inicio.countDown();
            assertEquals("CANCELACION_OK", cancelacion.get());
            String resultadoPago = pago.get();
            assertTrue("PAGO_PAGADA".equals(resultadoPago) || "PAGO_409".equals(resultadoPago), resultadoPago);
        } finally {
            executor.shutdownNow();
        }

        entityManager.clear();
        Compra compraFinal = compraRepository.findById(creada.idCompra()).orElseThrow();
        List<Entrada> entradasFinales = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        List<Pago> pagos = pagoRepository.findAll();
        FilaEvento fila = filaEventoRepository.findById(20L).orElseThrow();
        assertEquals("CERRADA", fila.getEstado());
        assertEquals("CANCELACION", fila.getCausaCierre());

        if (Compra.DEVUELTA.equals(compraFinal.getEstado())) {
            assertEquals(1, pagos.size());
            assertEquals("REEMBOLSADO", pagos.getFirst().getEstado());
            assertTrue(entradasFinales.stream().allMatch(entrada -> "DEVUELTA".equals(entrada.getEstado())));
        } else {
            assertEquals(Compra.CANCELADA, compraFinal.getEstado());
            assertTrue(pagos.isEmpty());
            assertTrue(entradasFinales.stream().allMatch(entrada -> "CANCELADA".equals(entrada.getEstado())));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosResultadosDePagoConcurrentesSoloDejanUnoGanador() throws Exception {
        prepararFila(4, 4, 10L);
        CompraResponse creada = compraService.crear(10L, "compra-carrera-resultados", request(2));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> aprobado = executor.submit(() -> intentoPago(
                    inicio, transaction, creada.idCompra(), "pago-carrera-aprobado", "APRUEBA"));
            Future<String> fallido = executor.submit(() -> intentoPago(
                    inicio, transaction, creada.idCompra(), "pago-carrera-fallido", "FALLA"));
            inicio.countDown();
            String resultadoAprobado = aprobado.get();
            String resultadoFallido = fallido.get();
            boolean ganoAprobado = resultadoAprobado.startsWith("OK:");
            boolean ganoFallido = resultadoFallido.startsWith("OK:");
            assertTrue(ganoAprobado != ganoFallido, resultadoAprobado + " / " + resultadoFallido);
        } finally {
            executor.shutdownNow();
        }

        entityManager.clear();
        Compra compraFinal = compraRepository.findById(creada.idCompra()).orElseThrow();
        List<Entrada> entradasFinales = entradaRepository.findByIdCompraOrderById(creada.idCompra());
        List<Pago> pagos = pagoRepository.findAll();
        assertEquals(1, pagos.size());
        if (Compra.PAGADA.equals(compraFinal.getEstado())) {
            assertEquals("APROBADO", pagos.getFirst().getEstado());
            assertTrue(entradasFinales.stream().allMatch(entrada -> "VALIDA".equals(entrada.getEstado())));
            assertTrue(entradasFinales.stream().allMatch(entrada -> entrada.getCodigoQr() != null));
        } else {
            assertEquals(Compra.RECHAZADA, compraFinal.getEstado());
            assertEquals("FALLIDO", pagos.getFirst().getEstado());
            assertTrue(entradasFinales.stream().allMatch(entrada -> "CANCELADA".equals(entrada.getEstado())));
            assertTrue(entradasFinales.stream().allMatch(entrada -> entrada.getCodigoQr() == null));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void secuenciaDuplicadaEnElMismoEventoEsRechazadaPorLaBase() {
        IngresoFila primera = ingreso(10L, 1L);
        IngresoFila segunda = ingreso(11L, 1L);

        assertThrows(DataIntegrityViolationException.class, () -> {
            ingresoFilaRepository.saveAndFlush(primera);
            ingresoFilaRepository.saveAndFlush(segunda);
        });
    }

    private IngresoFila ingreso(Long idUsuario, Long secuencia) {
        IngresoFila ingreso = new IngresoFila();
        ingreso.setIdEvento(20L);
        ingreso.setIdUsuario(idUsuario);
        ingreso.setSecuencia(secuencia);
        ingreso.setEstado("EN_ESPERA");
        ingreso.setIngresadoEn(Instant.now());
        return ingreso;
    }

    private CompraRequest request() {
        return request(1);
    }

    private CompraRequest request(int cantidad) {
        List<EntradaCompraRequest> selecciones = new java.util.ArrayList<>();
        for (int indice = 0; indice < cantidad; indice++) {
            selecciones.add(new EntradaCompraRequest(30L, null, null));
        }
        return new CompraRequest(20L, selecciones);
    }

    private String intentoPago(
            CountDownLatch inicio,
            TransactionTemplate transaction,
            Long idCompra,
            String clave,
            String resultado
    ) throws InterruptedException {
        inicio.await();
        try {
            PagoCompraResponse respuesta = transaction.execute(status -> servicioConResultado(resultado).pagar(
                    idCompra, 10L, clave, new PagoCompraRequest("TARJETA")
            ));
            return "OK:" + respuesta.estado();
        } catch (ResponseStatusException exception) {
            return "CONFLICTO:" + exception.getStatusCode().value();
        }
    }

    private CancelacionEventoService cancelacionConResultado(String resultado) {
        EntradaService entradas = new EntradaService(
                entradaRepository, filaEventoRepository, usuarioClient, eventoClient, sectorClient, 6
        );
        PagoService pagos = new PagoService(
                pagoRepository, entradaRepository, filaEventoRepository, registroPagoRepository, resultado
        );
        return new CancelacionEventoService(entradas, pagos, filaConResultado(resultado));
    }

    private CompraService servicioConResultado(String resultado) {
        CompraService servicio = new CompraService(
                compraRepository, entradaRepository, filaEventoRepository, ingresoFilaRepository,
                intentoCompraRepository, tandaFilaRepository, pagoRepository, registroPagoRepository,
                eventoClient, sectorClient, eventPublisher, resultado, 6
        );
        ReflectionTestUtils.setField(servicio, "entityManager", entityManager);
        return servicio;
    }

    private FilaVirtualService filaConResultado(String resultado) {
        return new FilaVirtualService(
                filaEventoRepository, ingresoFilaRepository, tandaFilaRepository, entradaRepository,
                eventoClient, sectorClient, servicioConResultado(resultado)
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
}
