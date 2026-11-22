package com.ticketya.preferencias.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un documento por cliente. El _id es el idUsuario de msvc-usuarios (referencia por id,
 * sin FK ni entidades de otros servicios). Las compras se registran una sola vez gracias
 * a {@code comprasProcesadas}; {@code eventosComprados} permite excluirlos de las
 * recomendaciones (EP-17).
 */
@Document(collection = "preferencias_cliente")
public class PreferenciaCliente {

    @Id
    @Field("_id")
    private Long idUsuario;

    private String ciudad;

    @Field("tipos_evento")
    private Map<String, Integer> tiposEvento = new LinkedHashMap<>();

    private List<ArtistaPreferido> artistas = new ArrayList<>();

    @Field("compras_procesadas")
    private Set<Long> comprasProcesadas = new LinkedHashSet<>();

    @Field("eventos_comprados")
    private Set<Long> eventosComprados = new LinkedHashSet<>();

    @Field("actualizado_en")
    private Instant actualizadoEn;

    public PreferenciaCliente() {
    }

    public PreferenciaCliente(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public Map<String, Integer> getTiposEvento() {
        return tiposEvento;
    }

    public void setTiposEvento(Map<String, Integer> tiposEvento) {
        this.tiposEvento = tiposEvento;
    }

    public List<ArtistaPreferido> getArtistas() {
        return artistas;
    }

    public void setArtistas(List<ArtistaPreferido> artistas) {
        this.artistas = artistas;
    }

    public Set<Long> getComprasProcesadas() {
        return comprasProcesadas;
    }

    public void setComprasProcesadas(Set<Long> comprasProcesadas) {
        this.comprasProcesadas = comprasProcesadas;
    }

    public Set<Long> getEventosComprados() {
        return eventosComprados;
    }

    public void setEventosComprados(Set<Long> eventosComprados) {
        this.eventosComprados = eventosComprados;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
