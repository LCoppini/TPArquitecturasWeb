package com.ticketya.evento.config;

import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eventos de demostración, solo con el perfil {@code dev-seed} y solo si la tabla está vacía.
 *
 * <p>Referencia lugares y sectores de msvc-ubicacion por id, sin consultarlo. Funciona si ese servicio
 * cargó antes su seed sobre una base vacía (lugares 1 a 4; sectores 1-3, 4-6, 7-8 y 9-10), y el organizador
 * es el usuario {@code seed.dev.id-organizador} (2 si msvc-usuarios cargó su seed sobre una base vacía).
 * Las fechas son relativas a hoy, así que la cartelera siempre tiene eventos futuros.
 */
@Component
@Profile("dev-seed")
public class DatosDemoEventoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoEventoSeeder.class);
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Sectores de cada lugar, en el orden en que los crea el seed de msvc-ubicacion. */
    private static final Map<Long, long[]> SECTORES_POR_LUGAR = Map.of(
            1L, new long[]{1, 2, 3},
            2L, new long[]{4, 5, 6},
            3L, new long[]{7, 8},
            4L, new long[]{9, 10}
    );

    /** Cada sector cuesta el precio base por su multiplicador, en el orden del lugar. */
    private static final double[] MULTIPLICADORES = {1.0, 1.5, 2.2};

    private record EventoDemo(String nombre, String tipo, long idLugar, int dias, int precioBase,
                              String estado, List<String> participantes, String descripcion) {
    }

    private static EventoDemo publicado(String nombre, String tipo, long idLugar, int dias, int precioBase,
                                        List<String> participantes, String descripcion) {
        return new EventoDemo(nombre, tipo, idLugar, dias, precioBase, EstadosEvento.PUBLICADO, participantes, descripcion);
    }

    private static final List<EventoDemo> EVENTOS = List.of(
            publicado("Noche de Rock Nacional", "Recital", 1, 12, 32000, List.of("Los Fugitivos"), "Una noche con los clásicos del rock nacional."),
            publicado("Festival Aurora", "Festival", 4, 20, 28000, List.of("Luna Verde", "Cielo Eléctrico"), "Dos escenarios y bandas en vivo hasta la madrugada."),
            publicado("Final del Torneo Federal", "Deporte", 1, 8, 18000, List.of("Club Atlético Sur"), "La gran final del torneo, con tribunas llenas."),
            publicado("El Último Tango", "Teatro", 2, 5, 14000, List.of("Compañía Escena Viva"), "Una obra sobre la nostalgia y los reencuentros."),
            publicado("Concierto Sinfónico de Primavera", "Recital", 2, 16, 21000, List.of("Orquesta del Centro"), "Un recorrido por las obras más queridas del repertorio."),
            publicado("Copa Ciudad de Tandil", "Deporte", 3, 10, 9000, List.of("Selección de Tandil"), "Cuadrangular de verano con equipos de la región."),
            publicado("Stand Up Sin Filtro", "Teatro", 2, 3, 11000, List.of("Mariano Paz"), "Humor, anécdotas y mucha improvisación."),
            publicado("Cumbia Fest", "Festival", 4, 25, 17000, List.of("Los del Río Sur"), "La fiesta más grande de la cumbia en Rosario."),
            publicado("Pop en el Arena", "Recital", 3, 14, 26000, List.of("Lucía Vera"), "Gira de presentación del nuevo disco."),
            publicado("Romeo y Julieta", "Teatro", 2, 22, 16000, List.of("Compañía Escena Viva"), "Una versión contemporánea del clásico."),
            publicado("Clásico de Verano", "Deporte", 1, 30, 22000, List.of("Club Atlético Sur", "Deportivo Norte"), "El partido más esperado de la temporada."),
            publicado("Electrónica al Aire Libre", "Festival", 4, 35, 30000, List.of("DJ Maraña"), "Música electrónica bajo las estrellas."),
            publicado("Trap Night", "Recital", 1, 18, 29000, List.of("Kiko B"), "Una noche de trap con invitados especiales."),
            publicado("Folklore bajo las Estrellas", "Recital", 4, 28, 15000, List.of("Las Voces del Monte"), "Peña folklórica con guitarras y baile."),
            publicado("Maratón Solidaria", "Deporte", 3, 40, 5000, List.of(), "Carrera de 10 km a beneficio de hospitales locales."),
            publicado("Comedia Musical: Brillos", "Teatro", 2, 45, 19000, List.of("Elenco Brillos"), "Una comedia musical para toda la familia."),
            publicado("Jazz en la Costanera", "Recital", 4, 50, 13000, List.of("Cuarteto Delta"), "Jazz en vivo frente al río."),
            publicado("Rock Sinfónico", "Recital", 3, 60, 27000, List.of("Los Fugitivos", "Orquesta del Centro"), "Las bandas clásicas con orquesta."),
            new EventoDemo("Noche de Tango Agotada", "Recital", 2, 6, 24000, EstadosEvento.AGOTADO, List.of("Cuarteto Delta"), "Función con entradas agotadas."),
            new EventoDemo("Recital Cancelado", "Recital", 1, 15, 25000, EstadosEvento.CANCELADO, List.of("Kiko B"), "Evento cancelado por el organizador."),
            new EventoDemo("Gira en Preparación", "Festival", 4, 90, 20000, EstadosEvento.BORRADOR, List.of(), "Evento todavía en borrador."),
            new EventoDemo("Clásico del Año Pasado", "Deporte", 1, -30, 15000, EstadosEvento.FINALIZADO, List.of("Deportivo Norte"), "Evento ya finalizado.")
    );

    private final EventoRepository eventoRepository;
    private final ParticipanteRepository participanteRepository;
    private final long idOrganizador;

    public DatosDemoEventoSeeder(
            EventoRepository eventoRepository,
            ParticipanteRepository participanteRepository,
            @Value("${seed.dev.id-organizador:2}") long idOrganizador
    ) {
        this.eventoRepository = eventoRepository;
        this.participanteRepository = participanteRepository;
        this.idOrganizador = idOrganizador;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (eventoRepository.count() > 0) {
            log.info("Datos de demostración de eventos omitidos: ya hay eventos cargados.");
            return;
        }
        Map<String, Participante> participantes = new HashMap<>();
        EVENTOS.forEach(demo -> eventoRepository.save(construir(demo, participantes)));
        log.info("Datos de demostración de eventos: {} creados (organizador {}).", EVENTOS.size(), idOrganizador);
    }

    private Evento construir(EventoDemo demo, Map<String, Participante> participantes) {
        Evento evento = new Evento();
        evento.setIdLugar(demo.idLugar());
        evento.setIdOrganizador(idOrganizador);
        evento.setNombre(demo.nombre());
        evento.setDescripcion(demo.descripcion());
        evento.setTipo(demo.tipo());
        evento.setFechaHora(fecha(demo.dias()));
        evento.setEstado(demo.estado());
        evento.reemplazarParticipantes(demo.participantes().stream()
                .map(nombre -> participante(nombre, demo.tipo(), participantes))
                .toList());
        evento.getSectores().addAll(sectores(evento, demo));
        return evento;
    }

    private List<EventoSector> sectores(Evento evento, EventoDemo demo) {
        long[] ids = SECTORES_POR_LUGAR.get(demo.idLugar());
        List<EventoSector> sectores = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            long precio = Math.round(demo.precioBase() * MULTIPLICADORES[i] / 500.0) * 500;
            sectores.add(new EventoSector(evento, ids[i], BigDecimal.valueOf(precio)));
        }
        return sectores;
    }

    private Participante participante(String nombre, String tipo, Map<String, Participante> cache) {
        return cache.computeIfAbsent(nombre, clave -> participanteRepository
                .findByNombreIgnoreCaseAndTipoIgnoreCase(clave, tipo)
                .orElseGet(() -> participanteRepository.save(new Participante(clave, tipo))));
    }

    /** 21:00 hora de Argentina, a tantos días de hoy. */
    private Instant fecha(int dias) {
        return LocalDate.now(ZONA).plusDays(dias).atTime(21, 0).atZone(ZONA).toInstant();
    }
}
