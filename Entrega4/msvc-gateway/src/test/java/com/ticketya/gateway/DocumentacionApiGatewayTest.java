package com.ticketya.gateway;

import com.ticketya.gateway.security.GatewayRoleMatrix;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los prefijos documentados en api-fila-virtual.md tienen ruta y regla de rol. */
class DocumentacionApiGatewayTest {

    private static final List<String> PREFIJOS = List.of(
            "/api/organizador/filas-virtuales/**",
            "/api/filas-virtuales/**",
            "/api/compras/**"
    );

    @Test
    void losPrefijosDocumentadosTienenReglaExplicitaDeRol() {
        List<String> conRegla = GatewayRoleMatrix.prefijosConRegla().stream()
                .map(prefijo -> prefijo + "/**")
                .toList();
        PREFIJOS.forEach(prefijo -> assertTrue(conRegla.contains(prefijo),
                "Prefijo documentado sin regla de rol: " + prefijo));
    }

    @Test
    void losPrefijosDocumentadosEstanEnElDocumentoYEnLasRutasDelGateway() throws IOException {
        String documento = Files.readString(Path.of("..", "docs", "api-fila-virtual.md"));
        String rutas = Files.readString(Path.of("..", "msvc-config", "src", "main", "resources",
                "configuration", "msvc-gateway.yml"));
        PREFIJOS.forEach(prefijo -> {
            assertTrue(documento.contains(prefijo), "Prefijo ausente en la documentación: " + prefijo);
            assertTrue(rutas.contains("Path=" + prefijo), "Prefijo sin ruta en el gateway: " + prefijo);
        });
    }
}
