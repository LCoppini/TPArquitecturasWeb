package com.ticketya.ubicacion.controller;

import com.ticketya.ubicacion.dto.response.LugarResponse;
import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.entity.TipoSector;
import com.ticketya.ubicacion.exception.RecursoNoEncontradoException;
import com.ticketya.ubicacion.exception.UbicacionExceptionHandler;
import com.ticketya.ubicacion.service.LugarService;
import com.ticketya.ubicacion.service.SectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LugarControllerTest {

    @Mock
    private LugarService lugarService;

    @Mock
    private SectorService sectorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LugarController(lugarService), new SectorController(sectorService))
                .setControllerAdvice(new UbicacionExceptionHandler())
                .build();
    }

    @Test
    void obtenerLugarExponeElContratoQueConsumeEvento() throws Exception {
        SectorResponse sector = new SectorResponse(31L, 4L, "Campo", TipoSector.NO_NUMERADO, null, null, 100);
        when(lugarService.obtenerPorId(4L)).thenReturn(new LugarResponse(
                4L, "Estadio", "Calle 1", "Tandil", "-37.3,-59.1", -37.3, -59.1, List.of(sector)));

        mockMvc.perform(get("/api/lugares/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.ciudad").value("Tandil"))
                .andExpect(jsonPath("$.ubicacion").value("-37.3,-59.1"))
                .andExpect(jsonPath("$.sectores[0].idSector").value(31));
    }

    @Test
    void obtenerSectorExponeElContratoQueConsumeEntradaSinPrecio() throws Exception {
        when(sectorService.obtenerPorId(31L)).thenReturn(
                new SectorResponse(31L, 4L, "Platea", TipoSector.NUMERADO, 5, 10, 50));

        mockMvc.perform(get("/api/sectores/31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSector").value(31))
                .andExpect(jsonPath("$.idLugar").value(4))
                .andExpect(jsonPath("$.tipo").value("NUMERADO"))
                .andExpect(jsonPath("$.filas").value(5))
                .andExpect(jsonPath("$.asientosPorFila").value(10))
                .andExpect(jsonPath("$.capacidadMax").value(50))
                .andExpect(jsonPath("$.precio").doesNotExist());
    }

    @Test
    void lugarInexistenteResponde404ConElFormatoDeErrorComun() throws Exception {
        when(lugarService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe el lugar con id 99"));

        mockMvc.perform(get("/api/lugares/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("No existe el lugar con id 99"))
                .andExpect(jsonPath("$.ruta").value("/api/lugares/99"));
    }

    @Test
    void crearLugarSinSectoresYConCoordenadasInvalidasResponde400ConElDetalleDeCadaError() throws Exception {
        String cuerpo = """
                {"nombre":"Estadio","direccion":"Calle 1","ciudad":"Tandil",
                 "latitud":120,"longitud":-59.1,"sectores":[]}
                """;

        mockMvc.perform(post("/api/lugares").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("latitud")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("sectores")));
    }

    @Test
    void crearLugarValidoResponde201() throws Exception {
        when(lugarService.crear(any())).thenReturn(new LugarResponse(
                1L, "Estadio", "Calle 1", "Tandil", "-37.3,-59.1", -37.3, -59.1, List.of()));
        String cuerpo = """
                {"nombre":"Estadio","direccion":"Calle 1","ciudad":"Tandil","latitud":-37.3,"longitud":-59.1,
                 "sectores":[{"nombre":"Campo","tipo":"NO_NUMERADO","capacidadMax":100}]}
                """;

        mockMvc.perform(post("/api/lugares").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void cuerpoMalFormadoResponde400() throws Exception {
        mockMvc.perform(post("/api/lugares").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest());
    }
}
