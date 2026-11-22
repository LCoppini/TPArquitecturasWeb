package com.ticketya.preferencias.repository;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.ticketya.preferencias.dto.request.ArtistaCompraRequest;
import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro idempotente mediante un único updateOne con pipeline de agregación.
 * El filtro condicional por idEntrada y la serialización de MongoDB sobre el mismo _id
 * hacen que dos compras concurrentes no pierdan incrementos.
 */
public class PreferenciaClienteRepositoryImpl implements PreferenciaClienteRepositoryCustom {

    private static final String COLECCION = "preferencias_cliente";

    private final MongoTemplate mongoTemplate;

    public PreferenciaClienteRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void registrarCompra(CompraRegistradaRequest compra) {
        Bson filtro = Filters.and(
                Filters.eq("_id", compra.idUsuario()),
                Filters.ne("compras_procesadas", compra.idEntrada()));
        List<Document> pipeline = construirPipeline(compra);
        UpdateOptions opciones = new UpdateOptions().upsert(true);

        try {
            collection().updateOne(filtro, pipeline, opciones);
        } catch (MongoWriteException ex) {
            if (ex.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
                throw ex;
            }
            // El documento ya existe (reintento de la misma entrada o carrera de dos
            // upserts). Sin upsert: si el filtro ya no coincide, es idempotencia y
            // no se inserta un duplicado; si coincide, se aplica el pipeline.
            collection().updateOne(filtro, pipeline);
        }
    }

    @Override
    public boolean existeCompra(Long idUsuario, Long idEntrada) {
        return collection().countDocuments(Filters.and(
                Filters.eq("_id", idUsuario),
                Filters.eq("compras_procesadas", idEntrada))) > 0;
    }

    private MongoCollection<Document> collection() {
        return mongoTemplate.getCollection(COLECCION);
    }

    private List<Document> construirPipeline(CompraRegistradaRequest compra) {
        Document primeraEtapa = new Document("$set", new Document()
                .append("compras_procesadas", new Document("$setUnion", List.of(
                        new Document("$ifNull", List.of("$compras_procesadas", List.of())),
                        List.of(compra.idEntrada()))))
                .append("eventos_comprados", new Document("$setUnion", List.of(
                        new Document("$ifNull", List.of("$eventos_comprados", List.of())),
                        List.of(compra.idEvento()))))
                .append("ciudad", compra.ciudad())
                .append("actualizado_en", Date.from(Instant.now())));

        Document segundaEtapa = new Document("$set",
                new Document("artistas", expresionArtistas(compra.artistas()))
                        .append("tipos_evento", expresionTipos(compra.tipoEvento())));

        return List.of(primeraEtapa, segundaEtapa);
    }

    /**
     * Un artista repetido dentro de la misma compra cuenta una sola vez. Se preserva el
     * orden de aparición para que el incremento sea determinístico.
     */
    private List<Document> artistasDistintos(List<ArtistaCompraRequest> artistas) {
        if (artistas == null) {
            return List.of();
        }
        Map<Long, ArtistaCompraRequest> porId = artistas.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ArtistaCompraRequest::idArtista, Function.identity(),
                        (primero, segundo) -> primero, LinkedHashMap::new));
        return porId.values().stream()
                .map(a -> new Document("id_artista", a.idArtista())
                        .append("nombre", a.nombre())
                        .append("cantidad", 1))
                .toList();
    }

    private Document expresionArtistas(List<ArtistaCompraRequest> artistas) {
        List<Document> nuevos = artistasDistintos(artistas);
        Document incrementar = new Document("$map", new Document("input", "$$value")
                .append("as", "a")
                .append("in", new Document("$cond", List.of(
                        new Document("$eq", List.of("$$a.id_artista", "$$this.id_artista")),
                        new Document("$mergeObjects", List.of("$$a",
                                new Document("cantidad", new Document("$add",
                                        List.of("$$a.cantidad", 1))))),
                        "$$a"))));
        Document agregar = new Document("$concatArrays", List.of("$$value",
                List.of(new Document("id_artista", "$$this.id_artista")
                        .append("nombre", "$$this.nombre")
                        .append("cantidad", 1))));

        return new Document("$reduce", new Document("input", nuevos)
                .append("initialValue", new Document("$ifNull", List.of("$artistas", List.of())))
                .append("in", new Document("$cond", List.of(
                        new Document("$in", List.of("$$this.id_artista", "$$value.id_artista")),
                        incrementar,
                        agregar))));
    }

    private Document expresionTipos(String tipoEvento) {
        Document tiposActuales = new Document("$objectToArray",
                new Document("$ifNull", List.of("$tipos_evento", new Document())));
        Document incrementar = new Document("$map", new Document("input", tiposActuales)
                .append("as", "t")
                .append("in", new Document("$cond", List.of(
                        new Document("$eq", List.of("$$t.k", tipoEvento)),
                        new Document("k", "$$t.k")
                                .append("v", new Document("$add", List.of("$$t.v", 1))),
                        "$$t"))));
        Document claves = new Document("$map", new Document("input", tiposActuales)
                .append("as", "t")
                .append("in", "$$t.k"));
        Document agregar = new Document("$cond", List.of(
                new Document("$in", List.of(tipoEvento, claves)),
                List.of(),
                List.of(new Document("k", tipoEvento).append("v", 1))));

        return new Document("$arrayToObject",
                new Document("$concatArrays", List.of(incrementar, agregar)));
    }
}
