package com.ticketya.preferencias.entity;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Subdocumento dentro de {@link PreferenciaCliente}. Guarda solo el identificador del
 * artista de msvc-evento, su nombre y la cantidad de compras en que apareció.
 */
public class ArtistaPreferido {

    @Field("id_artista")
    private Long idArtista;

    private String nombre;

    private int cantidad;

    public ArtistaPreferido() {
    }

    public ArtistaPreferido(Long idArtista, String nombre, int cantidad) {
        this.idArtista = idArtista;
        this.nombre = nombre;
        this.cantidad = cantidad;
    }

    public Long getIdArtista() {
        return idArtista;
    }

    public void setIdArtista(Long idArtista) {
        this.idArtista = idArtista;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
