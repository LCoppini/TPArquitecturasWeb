package com.ticketya.ubicacion.config;

import com.ticketya.ubicacion.entity.Lugar;
import com.ticketya.ubicacion.entity.Sector;
import com.ticketya.ubicacion.entity.TipoSector;
import com.ticketya.ubicacion.repository.LugarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lugares y sectores de demostración, solo con el perfil {@code dev-seed} y solo si la tabla está vacía.
 * En una base vacía los ids quedan fijos y el seed de msvc-evento depende de ellos:
 * lugares 1 a 4; sectores 1-3 (lugar 1), 4-6 (lugar 2), 7-8 (lugar 3) y 9-10 (lugar 4).
 */
@Component
@Profile("dev-seed")
public class DatosDemoUbicacionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoUbicacionSeeder.class);

    private record SectorDemo(String nombre, TipoSector tipo, Integer filas, Integer asientosPorFila, int capacidad) {
        static SectorDemo numerado(String nombre, int filas, int asientosPorFila) {
            return new SectorDemo(nombre, TipoSector.NUMERADO, filas, asientosPorFila, filas * asientosPorFila);
        }

        static SectorDemo zona(String nombre, int capacidad) {
            return new SectorDemo(nombre, TipoSector.NO_NUMERADO, null, null, capacidad);
        }
    }

    private record LugarDemo(String nombre, String direccion, String ciudad, double latitud, double longitud,
                             List<SectorDemo> sectores) {
    }

    private static final List<LugarDemo> LUGARES = List.of(
            new LugarDemo("Estadio Monumental del Sur", "Av. del Libertador 7500", "Buenos Aires", -34.5453, -58.4498,
                    List.of(SectorDemo.zona("Campo", 4000),
                            SectorDemo.numerado("Platea Baja", 20, 30),
                            SectorDemo.numerado("Platea Alta", 20, 40))),
            new LugarDemo("Teatro del Centro", "Av. Corrientes 1200", "Buenos Aires", -34.6037, -58.3816,
                    List.of(SectorDemo.numerado("Platea", 25, 20),
                            SectorDemo.numerado("Pullman", 10, 20),
                            SectorDemo.numerado("Balcón", 12, 25))),
            new LugarDemo("Arena Tandil", "Ruta 226 km 3", "Tandil", -37.3217, -59.1332,
                    List.of(SectorDemo.zona("Campo", 1500),
                            SectorDemo.numerado("Tribuna", 15, 30))),
            new LugarDemo("Anfiteatro del Río", "Costanera Norte 450", "Rosario", -32.9468, -60.6393,
                    List.of(SectorDemo.zona("Césped", 3000),
                            SectorDemo.numerado("Platea", 20, 25)))
    );

    private final LugarRepository lugarRepository;

    public DatosDemoUbicacionSeeder(LugarRepository lugarRepository) {
        this.lugarRepository = lugarRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (lugarRepository.count() > 0) {
            log.info("Datos de demostración de ubicación omitidos: ya hay lugares cargados.");
            return;
        }
        for (LugarDemo demo : LUGARES) {
            Lugar lugar = new Lugar(demo.nombre(), demo.direccion(), demo.ciudad(), demo.latitud(), demo.longitud());
            demo.sectores().forEach(sector -> lugar.agregarSector(construir(sector)));
            lugarRepository.saveAndFlush(lugar);
        }
        log.info("Datos de demostración de ubicación: {} lugares creados.", LUGARES.size());
    }

    private Sector construir(SectorDemo demo) {
        Sector sector = new Sector();
        sector.setNombre(demo.nombre());
        sector.setTipo(demo.tipo());
        sector.setFilas(demo.filas());
        sector.setAsientosPorFila(demo.asientosPorFila());
        sector.setCapacidadMax(demo.capacidad());
        return sector;
    }
}
