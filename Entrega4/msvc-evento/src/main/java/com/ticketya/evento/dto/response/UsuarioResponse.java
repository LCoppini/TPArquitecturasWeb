package com.ticketya.evento.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.List;

public record UsuarioResponse(
        @JsonAlias({"id", "id_usuario"}) Long idUsuario,
        @JsonAlias({"roles", "usuarioRoles"})
        @JsonDeserialize(contentUsing = UsuarioResponse.RolDeserializer.class) List<String> roles
) {

    /**
     * Msvc-usuarios devuelve cada rol como objeto ({"idRol": 2, "tipo": "ORGANIZADOR"}).
     * Se acepta tambien un rol como texto plano para no romper otros formatos.
     */
    public static class RolDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode nodo = context.readTree(parser);
            if (nodo.isTextual()) {
                return nodo.asText();
            }
            if (nodo.hasNonNull("tipo")) {
                return nodo.get("tipo").asText();
            }
            if (nodo.hasNonNull("nombre")) {
                return nodo.get("nombre").asText();
            }
            return null;
        }
    }
}
