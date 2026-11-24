package com.ticketya.preferencias.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Busqueda de clientes por artista y ciudad contra el MongoDB de docker-compose (ADR-0002).
 * Ejecutar con: PREFERENCIAS_MONGO_INTEGRATION=true mvn -pl msvc-preferencias test
 */
@EnabledIfEnvironmentVariable(named = "PREFERENCIAS_MONGO_INTEGRATION", matches = "true")
class PreferenciaClienteBusquedaIntegrationTest {

    private static final MongoClient CLIENT = MongoClients.create("mongodb://localhost:27017");
    private static final MongoTemplate TEMPLATE = new MongoTemplate(CLIENT, "ticketya_preferencias");

    private final PreferenciaClienteRepositoryImpl repository = new PreferenciaClienteRepositoryImpl(TEMPLATE);

    private MongoCollection<Document> collection() {
        return TEMPLATE.getCollection("preferencias_cliente");
    }

    @BeforeEach
    void limpiar() {
        collection().deleteMany(new Document());
    }

    @AfterAll
    static void cerrar() {
        CLIENT.close();
    }

    @Test
    void devuelveSoloLosClientesDelArtistaYLaCiudadSinDistinguirMayusculasNiAcentos() {
        insertar(2L, "Córdoba", 7L);
        insertar(1L, "CORDOBA", 7L);
        insertar(3L, "Córdoba", 8L);
        insertar(4L, "Tandil", 7L);

        List<Long> clientes = repository.buscarClientesPorArtistaYCiudad(7L, "cordoba");

        assertEquals(List.of(1L, 2L), clientes);
    }

    private void insertar(Long idUsuario, String ciudad, Long idArtista) {
        collection().insertOne(new Document("_id", idUsuario)
                .append("ciudad", ciudad)
                .append("artistas", List.of(new Document("id_artista", idArtista).append("nombre", "Banda").append("cantidad", 1))));
    }
}
