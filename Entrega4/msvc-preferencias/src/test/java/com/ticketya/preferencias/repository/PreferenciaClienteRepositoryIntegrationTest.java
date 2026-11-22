package com.ticketya.preferencias.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.ticketya.preferencias.dto.request.ArtistaCompraRequest;
import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.entity.PreferenciaCliente;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integración contra el MongoDB levantado por docker-compose (ADR-0002, sin Testcontainers).
 * Ejecutar con: PREFERENCIAS_MONGO_INTEGRATION=true mvn -pl msvc-preferencias test
 */
@EnabledIfEnvironmentVariable(named = "PREFERENCIAS_MONGO_INTEGRATION", matches = "true")
class PreferenciaClienteRepositoryIntegrationTest {

    private static final MongoClient CLIENT = MongoClients.create("mongodb://localhost:27017");
    private static final MongoTemplate TEMPLATE = new MongoTemplate(CLIENT, "ticketya_preferencias");

    private final PreferenciaClienteRepositoryImpl repository =
            new PreferenciaClienteRepositoryImpl(TEMPLATE);

    private MongoCollection<Document> collection() {
        return TEMPLATE.getCollection("preferencias_cliente");
    }

    @BeforeEach
    void limpiarColeccion() {
        collection().deleteMany(new Document());
    }

    @AfterAll
    static void cerrarCliente() {
        CLIENT.close();
    }

    @Test
    void primeraCompraConArtistaNuevo() {
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(42L, preferencia.getIdUsuario());
        assertEquals("Tandil", preferencia.getCiudad());
        assertEquals(1, preferencia.getTiposEvento().get("SHOW"));
        assertEquals(1, preferencia.getArtistas().size());
        assertEquals(7L, preferencia.getArtistas().get(0).getIdArtista());
        assertEquals(1, preferencia.getArtistas().get(0).getCantidad());
        assertEquals(Set.of(101L), preferencia.getComprasProcesadas());
    }

    @Test
    void segundaCompraConArtistaYaExistenteIncrementaCantidad() {
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));
        repository.registrarCompra(compra(42L, 102L, 21L, "SHOW", "Tandil", 7L, "Banda"));

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(2, preferencia.getTiposEvento().get("SHOW"));
        assertEquals(1, preferencia.getArtistas().size());
        assertEquals(2, preferencia.getArtistas().get(0).getCantidad());
        assertEquals(Set.of(101L, 102L), preferencia.getComprasProcesadas());
    }

    @Test
    void dosComprasConcurrentesConElMismoArtistaNuevoNoPierdenIncrementos() throws Exception {
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch fin = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (long idEntrada : List.of(101L, 102L)) {
                executor.submit(() -> {
                    try {
                        listos.countDown();
                        listos.await();
                        repository.registrarCompra(
                                compra(42L, idEntrada, 20L, "SHOW", "Tandil", 7L, "Banda"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        fin.countDown();
                    }
                });
            }
            assertTrue(fin.await(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(1, preferencia.getArtistas().size());
        assertEquals(2, preferencia.getArtistas().get(0).getCantidad());
        assertEquals(2, preferencia.getTiposEvento().get("SHOW"));
        assertEquals(Set.of(101L, 102L), preferencia.getComprasProcesadas());
    }

    @Test
    void reintentarLaMismaEntradaNoIncrementaContadores() {
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(1, preferencia.getTiposEvento().get("SHOW"));
        assertEquals(1, preferencia.getArtistas().get(0).getCantidad());
        assertTrue(repository.existeCompra(42L, 101L));
        assertFalse(repository.existeCompra(42L, 999L));
    }

    @Test
    void artistaRepetidoDentroDeUnaCompraCuentaUnaSolaVez() {
        CompraRegistradaRequest compra = new CompraRegistradaRequest(42L, 101L, 20L, "SHOW", "Tandil",
                List.of(new ArtistaCompraRequest(7L, "Banda"),
                        new ArtistaCompraRequest(7L, "Banda"),
                        new ArtistaCompraRequest(8L, "Solista")));
        repository.registrarCompra(compra);

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(2, preferencia.getArtistas().size());
        assertEquals(1, preferencia.getArtistas().stream()
                .filter(a -> a.getIdArtista().equals(7L)).findFirst().orElseThrow().getCantidad());
        assertEquals(1, preferencia.getArtistas().stream()
                .filter(a -> a.getIdArtista().equals(8L)).findFirst().orElseThrow().getCantidad());
    }

    @Test
    void nuevaCompraActualizaLaCiudadVigenteDelUsuario() {
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));
        repository.registrarCompra(compra(42L, 102L, 21L, "TEATRO", "Buenos Aires", 7L, "Banda"));

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals("Buenos Aires", preferencia.getCiudad());
    }

    @Test
    void eventosCompradosQuedaPobladoSinDuplicados() {
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));
        repository.registrarCompra(compra(42L, 102L, 20L, "SHOW", "Tandil", 7L, "Banda"));
        repository.registrarCompra(compra(42L, 101L, 20L, "SHOW", "Tandil", 7L, "Banda"));

        PreferenciaCliente preferencia = TEMPLATE.findById(42L, PreferenciaCliente.class);

        assertEquals(Set.of(20L), preferencia.getEventosComprados());
    }

    private CompraRegistradaRequest compra(Long idUsuario, Long idEntrada, Long idEvento,
                                           String tipo, String ciudad, Long idArtista, String nombre) {
        return new CompraRegistradaRequest(idUsuario, idEntrada, idEvento, tipo, ciudad,
                List.of(new ArtistaCompraRequest(idArtista, nombre)));
    }
}
