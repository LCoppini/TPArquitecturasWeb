package com.ticketya.preferencias;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class OpenApiDocumentacionTest {

    @Autowired
    private OpenAPI openAPI;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elBeanOpenApiDeclaraElEsquemaBearer() {
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
        assertEquals("bearer", openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
    }

    @Test
    void elDocumentoPublicaTodosLosEndpointsDelChange() throws Exception {
        String respuesta = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ticketya - msvc-preferencias"))
                .andExpect(jsonPath("$.paths['/internal/preferencias/compras'].post").exists())
                .andExpect(jsonPath("$.paths['/api/preferencias/clientes/{idUsuario}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/preferencias/clientes/{idUsuario}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/recomendaciones/clientes/{idUsuario}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/recomendaciones/clientes/{idUsuario}'].get").exists())
                .andReturn().getResponse().getContentAsString();

        assertTrue(respuesta.contains("bearerAuth"));
    }
}
