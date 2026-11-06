package com.ticketya.entrada.integration;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.service.EntradaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EntradaService.class)
@Testcontainers
class NoDobleVentaTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private EntradaService entradaService;

    @Autowired
    private EntradaRepository entradaRepository;

    @MockitoBean
    private UsuarioClient usuarioClient;

    @MockitoBean
    private EventoClient eventoClient;

    @MockitoBean
    private SectorClient sectorClient;

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void configurarServiciosRemotos() {
        when(usuarioClient.obtenerPorId(10L)).thenReturn(new UsuarioResponse(10L));
        when(eventoClient.obtenerPorId(20L)).thenReturn(
                new EventoResponse(20L, Instant.now().plusSeconds(86_400), "PUBLICADO")
        );
        when(sectorClient.obtenerPorId(30L)).thenReturn(
                new SectorResponse(30L, 2, 2, 2, "NUMERADO")
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosReservasConcurrentesDelMismoAsientoSoloPermitenUnaVenta() throws Exception {
        // Usa PostgreSQL real para comprobar que el bloqueo por evento/sector impide vender dos veces el mismo asiento.
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primera = ejecutor.submit(() -> reservarAlMismoTiempo(inicio, "clave-a"));
            Future<Boolean> segunda = ejecutor.submit(() -> reservarAlMismoTiempo(inicio, "clave-b"));
            inicio.countDown();

            int reservasExitosas = (primera.get() ? 1 : 0) + (segunda.get() ? 1 : 0);

            assertEquals(1, reservasExitosas);
            assertEquals(1, entradaRepository.contarAsignacionesActivasDeAsiento(
                    20L, 30L, 1, 1, Instant.now()
            ));
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private boolean reservarAlMismoTiempo(CountDownLatch inicio, String clave) throws Exception {
        inicio.await();
        try {
            entradaService.crear(solicitud(clave));
            return true;
        } catch (IllegalStateException conflicto) {
            return false;
        }
    }

    private EntradaRequest solicitud(String clave) {
        EntradaRequest request = new EntradaRequest();
        request.setIdEvento(20L);
        request.setIdUsuario(10L);
        request.setIdSector(30L);
        request.setFila(1);
        request.setAsiento(1);
        request.setPrecio(new BigDecimal("100.00"));
        request.setClaveIdempotencia(clave);
        return request;
    }
}
