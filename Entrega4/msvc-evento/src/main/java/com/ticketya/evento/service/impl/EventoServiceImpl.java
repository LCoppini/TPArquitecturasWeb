package com.ticketya.evento.service.impl;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.LugarClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.client.UsuarioClient;
import com.ticketya.evento.dto.request.ConfigurarSectoresRequest;
import com.ticketya.evento.dto.request.EventoRequest;
import com.ticketya.evento.dto.request.EventoSectorRequest;
import com.ticketya.evento.dto.request.ParticipanteRequest;
import com.ticketya.evento.dto.response.*;
import com.ticketya.evento.entity.EstadosEvento;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.Participante;
import com.ticketya.evento.exception.AccesoDenegadoException;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.exception.ReglaNegocioException;
import com.ticketya.evento.exception.ServicioExternoException;
import com.ticketya.evento.mapper.EventoMapper;
import com.ticketya.evento.repository.EventoRepository;
import com.ticketya.evento.repository.EventoSectorRepository;
import com.ticketya.evento.repository.ParticipanteRepository;
import com.ticketya.evento.service.EventoService;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class EventoServiceImpl implements EventoService {

    private static final String ROL_ORGANIZADOR = "ORGANIZADOR";
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final EventoRepository eventoRepository;
    private final EventoSectorRepository eventoSectorRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioClient usuarioClient;
    private final LugarClient lugarClient;
    private final SectorClient sectorClient;
    private final EntradaClient entradaClient;
    private final EventoMapper eventoMapper;

    public EventoServiceImpl(
            EventoRepository eventoRepository,
            EventoSectorRepository eventoSectorRepository,
            ParticipanteRepository participanteRepository,
            UsuarioClient usuarioClient,
            LugarClient lugarClient,
            SectorClient sectorClient,
            EntradaClient entradaClient,
            EventoMapper eventoMapper
    ) {
        this.eventoRepository = eventoRepository;
        this.eventoSectorRepository = eventoSectorRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioClient = usuarioClient;
        this.lugarClient = lugarClient;
        this.sectorClient = sectorClient;
        this.entradaClient = entradaClient;
        this.eventoMapper = eventoMapper;
    }

    @Override
    @Transactional
    public EventoResponse crear(Long idSolicitante, EventoRequest request) {
        validarRol(idSolicitante, ROL_ORGANIZADOR);
        validarLugar(request.idLugar());

        Evento evento = new Evento();
        aplicarDatos(evento, request);
        evento.setIdOrganizador(idSolicitante);
        evento.setEstado(EstadosEvento.BORRADOR);
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    public EventoResponse obtenerPorId(Long id) {
        return eventoMapper.aResponse(buscarEvento(id));
    }

    @Override
    @Transactional
    public EventoResponse actualizar(Long id, Long idSolicitante, EventoRequest request) {
        Evento evento = buscarEvento(id);
        validarOrganizadorPropietario(evento, idSolicitante);
        if (!EstadosEvento.BORRADOR.equals(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden editar eventos en estado BORRADOR");
        }
        validarLugar(request.idLugar());
        if (!evento.getIdLugar().equals(request.idLugar())) {
            evento.getSectores().clear();
        }
        aplicarDatos(evento, request);
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public EventoResponse configurarSectores(
            Long id,
            Long idSolicitante,
            ConfigurarSectoresRequest request
    ) {
        Evento evento = buscarEvento(id);
        validarOrganizadorPropietario(evento, idSolicitante);
        if (!EstadosEvento.BORRADOR.equals(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden configurar sectores en estado BORRADOR");
        }

        Map<Long, EventoSectorRequest> solicitados = new LinkedHashMap<>();
        for (EventoSectorRequest sectorRequest : request.sectores()) {
            if (solicitados.putIfAbsent(sectorRequest.idSector(), sectorRequest) != null) {
                throw new IllegalArgumentException("No se puede repetir un sector en la configuración del evento");
            }
            validarSectorDelLugar(sectorRequest.idSector(), evento.getIdLugar());
        }

        Map<Long, EventoSector> existentes = new LinkedHashMap<>();
        evento.getSectores().forEach(sector -> existentes.put(sector.getIdSector(), sector));
        evento.getSectores().removeIf(sector -> !solicitados.containsKey(sector.getIdSector()));
        for (EventoSectorRequest sectorRequest : solicitados.values()) {
            EventoSector existente = existentes.get(sectorRequest.idSector());
            if (existente != null) {
                existente.setPrecio(sectorRequest.precio());
            } else {
                evento.getSectores().add(
                        new EventoSector(evento, sectorRequest.idSector(), sectorRequest.precio())
                );
            }
        }
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    public EventoSectorResponse obtenerPrecioSector(Long idEvento, Long idSector) {
        buscarEvento(idEvento);
        EventoSector eventoSector = eventoSectorRepository.findByIdIdEventoAndIdIdSector(idEvento, idSector)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El sector " + idSector + " no está configurado para el evento " + idEvento
                ));
        return new EventoSectorResponse(eventoSector.getIdSector(), eventoSector.getPrecio());
    }

    @Override
    @Transactional
    public EventoResponse publicar(Long id, Long idSolicitante) {
        Evento evento = buscarEvento(id);
        validarOrganizadorPropietario(evento, idSolicitante);
        if (!EstadosEvento.BORRADOR.equals(evento.getEstado())
                && !EstadosEvento.SUSPENDIDO.equals(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se puede publicar un evento en BORRADOR o SUSPENDIDO");
        }

        validarLugar(evento.getIdLugar());
        if (evento.getSectores().isEmpty()) {
            throw new ReglaNegocioException("El evento debe tener sectores y precios configurados");
        }
        for (EventoSector sector : evento.getSectores()) {
            if (sector.getPrecio() == null || sector.getPrecio().signum() < 0) {
                throw new ReglaNegocioException("Todos los sectores deben tener un precio válido");
            }
            validarSectorDelLugar(sector.getIdSector(), evento.getIdLugar());
        }

        evento.setEstado(EstadosEvento.PUBLICADO);
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public EventoResponse suspender(Long id, Long idSolicitante) {
        validarRol(idSolicitante, ROL_ADMINISTRADOR);
        Evento evento = buscarEvento(id);
        if (!EstadosEvento.PUBLICADO.equals(evento.getEstado())
                && !EstadosEvento.AGOTADO.equals(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se pueden suspender eventos publicados o agotados");
        }
        evento.setEstado(EstadosEvento.SUSPENDIDO);
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public EventoResponse cambiarEstadoAdministrativo(Long id, Long idSolicitante, String estado) {
        validarRol(idSolicitante, ROL_ADMINISTRADOR);
        Evento evento = buscarEvento(id);
        String estadoNormalizado = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        if (EstadosEvento.AGOTADO.equals(estadoNormalizado)) {
            if (!EstadosEvento.PUBLICADO.equals(evento.getEstado())) {
                throw new ReglaNegocioException("Solo un evento PUBLICADO puede pasar a AGOTADO");
            }
        } else if (EstadosEvento.FINALIZADO.equals(estadoNormalizado)) {
            if (!EstadosEvento.PUBLICADO.equals(evento.getEstado())
                    && !EstadosEvento.AGOTADO.equals(evento.getEstado())
                    && !EstadosEvento.SUSPENDIDO.equals(evento.getEstado())) {
                throw new ReglaNegocioException("El estado actual no permite finalizar el evento");
            }
            if (evento.getFechaHora().isAfter(Instant.now())) {
                throw new ReglaNegocioException("No se puede finalizar un evento cuya fecha todavía no ocurrió");
            }
        } else {
            throw new IllegalArgumentException("Este endpoint solo permite establecer AGOTADO o FINALIZADO");
        }
        evento.setEstado(estadoNormalizado);
        return eventoMapper.aResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional(noRollbackFor = ServicioExternoException.class)
    public EventoResponse cancelar(Long id, Long idSolicitante) {
        validarRol(idSolicitante, ROL_ADMINISTRADOR);
        Evento evento = buscarEvento(id);
        if (EstadosEvento.CANCELADO.equals(evento.getEstado())
                || EstadosEvento.FINALIZADO.equals(evento.getEstado())) {
            throw new ReglaNegocioException("No se puede cancelar un evento cancelado o finalizado");
        }

        evento.setEstado(EstadosEvento.CANCELADO);
        Evento guardado = eventoRepository.saveAndFlush(evento);
        try {
            entradaClient.cancelarEntradas(id, idSolicitante, ROL_ADMINISTRADOR);
        } catch (FeignException exception) {
            throw new ServicioExternoException(
                    "El evento quedó CANCELADO, pero Entrada no pudo procesar los reintegros; "
                            + "se debe reintentar la coordinación",
                    exception
            );
        }
        return eventoMapper.aResponse(guardado);
    }

    @Override
    public EventoResponse reintentarCoordinacionCancelacion(Long id, Long idSolicitante) {
        validarRol(idSolicitante, ROL_ADMINISTRADOR);
        Evento evento = buscarEvento(id);
        if (!EstadosEvento.CANCELADO.equals(evento.getEstado())) {
            throw new ReglaNegocioException("Solo se puede reintentar la coordinación de un evento CANCELADO");
        }
        try {
            entradaClient.cancelarEntradas(id, idSolicitante, ROL_ADMINISTRADOR);
        } catch (FeignException exception) {
            throw new ServicioExternoException(
                    "Entrada no pudo completar la coordinación de cancelación; se puede reintentar",
                    exception
            );
        }
        return eventoMapper.aResponse(evento);
    }

    @Override
    @Transactional
    public void eliminarFisicamente(Long id, Long idSolicitante) {
        Evento evento = buscarEvento(id);
        validarPropietarioOAdministrador(evento, idSolicitante);
        if (!EstadosEvento.BORRADOR.equals(evento.getEstado())) {
            throw new ReglaNegocioException(
                    "Solo se pueden eliminar físicamente eventos en BORRADOR; los demás deben conservar su historial"
            );
        }
        eventoRepository.delete(evento);
    }

    @Override
    public EventoPaginaResponse buscar(
            String tipo,
            Long idOrganizador,
            String participante,
            String ciudad,
            Instant desde,
            Instant hasta,
            String orden,
            int pagina,
            int tamano
    ) {
        validarPaginacion(pagina, tamano);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a desde");
        }

        String ordenBusqueda = normalizarOrden(orden);
        boolean sinFiltros = estaVacio(tipo) && idOrganizador == null && estaVacio(participante)
                && estaVacio(ciudad) && desde == null && hasta == null;
        Instant ahora = Instant.now();

        List<Evento> eventos = eventoRepository.findAll().stream()
                .filter(evento -> EstadosEvento.PUBLICADO.equals(evento.getEstado()))
                .filter(evento -> !sinFiltros || !evento.getFechaHora().isBefore(ahora))
                .filter(evento -> estaVacio(tipo) || coincide(evento.getTipo(), tipo))
                .filter(evento -> idOrganizador == null || idOrganizador.equals(evento.getIdOrganizador()))
                .filter(evento -> estaVacio(participante) || coincideParticipante(evento, participante))
                .filter(evento -> desde == null || !evento.getFechaHora().isBefore(desde))
                .filter(evento -> hasta == null || !evento.getFechaHora().isAfter(hasta))
                .toList();

        Map<Long, BigDecimal> precios = new LinkedHashMap<>();
        Map<Long, Long> ventas = new LinkedHashMap<>();
        List<Evento> filtrados = new ArrayList<>();
        for (Evento evento : eventos) {
            if (!estaVacio(ciudad)) {
                LugarResponse lugar = obtenerLugar(evento.getIdLugar());
                if (!coincide(lugar.ciudad(), ciudad) && !coincide(lugar.ubicacion(), ciudad)) {
                    continue;
                }
            }
            if (usaPrecio(ordenBusqueda)) {
                precios.put(evento.getId(), precioMinimo(evento));
            } else if ("relevancia".equals(ordenBusqueda)) {
                ventas.put(evento.getId(), cantidadVendida(evento));
            }
            filtrados.add(evento);
        }

        Comparator<Evento> comparador = switch (ordenBusqueda) {
            case "precioasc" -> Comparator.comparing(evento -> precios.get(evento.getId()));
            case "preciodesc" -> Comparator.comparing(
                    (Evento evento) -> precios.get(evento.getId()), Comparator.reverseOrder()
            );
            case "relevancia" -> Comparator.comparing(
                    (Evento evento) -> ventas.get(evento.getId()), Comparator.reverseOrder()
            ).thenComparing(Evento::getFechaHora);
            default -> Comparator.comparing(Evento::getFechaHora);
        };
        filtrados.sort(comparador.thenComparing(Evento::getId));

        long total = filtrados.size();
        int inicio = (int) Math.min((long) pagina * tamano, filtrados.size());
        int fin = Math.min(inicio + tamano, filtrados.size());
        List<EventoResponse> contenido = filtrados.subList(inicio, fin).stream()
                .map(eventoMapper::aResponse)
                .toList();
        int paginas = (int) Math.ceil((double) total / tamano);
        return new EventoPaginaResponse(contenido, pagina, tamano, total, paginas);
    }

    private void aplicarDatos(Evento evento, EventoRequest request) {
        evento.setIdLugar(request.idLugar());
        evento.setNombre(request.nombre().trim());
        evento.setDescripcion(request.descripcion().trim());
        evento.setTipo(request.tipo().trim());
        evento.setFechaHora(request.fechaHora());
        evento.reemplazarParticipantes(resolverParticipantes(request.participantes()));
    }

    private List<Participante> resolverParticipantes(List<ParticipanteRequest> solicitudes) {
        Map<Long, Participante> participantes = new LinkedHashMap<>();
        for (ParticipanteRequest solicitud : solicitudes) {
            if (solicitud == null) {
                throw new IllegalArgumentException("La lista de participantes no puede contener elementos nulos");
            }
            Participante participante;
            if (solicitud.id() != null) {
                if (!estaVacio(solicitud.nombre()) || !estaVacio(solicitud.tipo())) {
                    throw new IllegalArgumentException("Un participante se referencia por ID o se crea por nombre y tipo");
                }
                participante = participanteRepository.findById(solicitud.id())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No existe el participante con id " + solicitud.id()
                        ));
            } else {
                if (estaVacio(solicitud.nombre()) || estaVacio(solicitud.tipo())) {
                    throw new IllegalArgumentException("Para crear un participante se requieren nombre y tipo");
                }
                String nombre = solicitud.nombre().trim();
                String tipo = solicitud.tipo().trim();
                participante = participanteRepository.findByNombreIgnoreCaseAndTipoIgnoreCase(nombre, tipo)
                        .orElseGet(() -> participanteRepository.save(new Participante(nombre, tipo)));
            }
            participantes.put(participante.getId(), participante);
        }
        return List.copyOf(participantes.values());
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el evento con id " + id));
    }

    private void validarOrganizadorPropietario(Evento evento, Long idSolicitante) {
        validarRol(idSolicitante, ROL_ORGANIZADOR);
        if (!evento.getIdOrganizador().equals(idSolicitante)) {
            throw new AccesoDenegadoException("Solo el organizador propietario puede modificar este evento");
        }
    }

    private void validarPropietarioOAdministrador(Evento evento, Long idSolicitante) {
        UsuarioResponse usuario = obtenerUsuario(idSolicitante);
        if (!idSolicitante.equals(usuario.idUsuario())) {
            throw new ServicioExternoException("Usuarios devolvió un identificador distinto al solicitado", null);
        }
        if (tieneRol(usuario, ROL_ADMINISTRADOR)) {
            return;
        }
        if (!tieneRol(usuario, ROL_ORGANIZADOR) || !evento.getIdOrganizador().equals(idSolicitante)) {
            throw new AccesoDenegadoException("Solo el organizador propietario o un administrador puede eliminar");
        }
    }

    private void validarRol(Long idSolicitante, String rol) {
        if (idSolicitante == null || idSolicitante <= 0) {
            throw new IllegalArgumentException("X-User-Id debe identificar un usuario válido");
        }
        UsuarioResponse usuario = obtenerUsuario(idSolicitante);
        if (!idSolicitante.equals(usuario.idUsuario())) {
            throw new ServicioExternoException("Usuarios devolvió un identificador distinto al solicitado", null);
        }
        if (!tieneRol(usuario, rol)) {
            throw new AccesoDenegadoException("El usuario no tiene el rol " + rol);
        }
    }

    private boolean tieneRol(UsuarioResponse usuario, String rolRequerido) {
        return usuario.roles() != null && usuario.roles().stream()
                .anyMatch(rol -> rolRequerido.equalsIgnoreCase(
                        rol != null && rol.startsWith("ROLE_") ? rol.substring(5) : rol
                ));
    }

    private UsuarioResponse obtenerUsuario(Long id) {
        try {
            UsuarioResponse usuario = usuarioClient.obtenerPorId(id);
            if (usuario == null) {
                throw new ServicioExternoException("Usuarios devolvió una respuesta vacía", null);
            }
            return usuario;
        } catch (FeignException.NotFound exception) {
            throw new RecursoNoEncontradoException("No existe el usuario con id " + id);
        } catch (FeignException exception) {
            throw new ServicioExternoException("No se pudo validar el usuario en msvc-usuarios", exception);
        }
    }

    private void validarLugar(Long idLugar) {
        LugarResponse lugar = obtenerLugar(idLugar);
        if (!idLugar.equals(lugar.id())) {
            throw new ServicioExternoException("Ubicación devolvió un identificador de lugar inesperado", null);
        }
    }

    private void validarSectorDelLugar(Long idSector, Long idLugarEsperado) {
        try {
            SectorResponse sector = sectorClient.obtenerPorId(idSector);
            if (sector == null || !idSector.equals(sector.idSector())) {
                throw new ServicioExternoException(
                        "Ubicación devolvió un identificador de sector inesperado",
                        null
                );
            }
            if (!idLugarEsperado.equals(sector.idLugar())) {
                throw new ReglaNegocioException(
                        "El sector " + idSector + " no pertenece al lugar del evento"
                );
            }
        } catch (FeignException.NotFound exception) {
            throw new RecursoNoEncontradoException("No existe el sector con id " + idSector);
        } catch (FeignException exception) {
            throw new ServicioExternoException("No se pudo consultar el sector en msvc-ubicacion", exception);
        }
    }

    private LugarResponse obtenerLugar(Long idLugar) {
        try {
            LugarResponse lugar = lugarClient.obtenerPorId(idLugar);
            if (lugar == null) {
                throw new ServicioExternoException("Ubicación devolvió una respuesta vacía para el lugar", null);
            }
            return lugar;
        } catch (FeignException.NotFound exception) {
            throw new RecursoNoEncontradoException("No existe el lugar con id " + idLugar);
        } catch (FeignException exception) {
            throw new ServicioExternoException("No se pudo consultar el lugar en msvc-ubicacion", exception);
        }
    }

    private BigDecimal precioMinimo(Evento evento) {
        BigDecimal precio = evento.getSectores().stream()
                .map(EventoSector::getPrecio)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        if (precio == null) {
            throw new ReglaNegocioException(
                    "El evento no tiene sectores con precio configurado: " + evento.getId()
            );
        }
        return precio;
    }

    private long cantidadVendida(Evento evento) {
        try {
            VentasEventoResponse ventas = entradaClient.consultarVentas(evento.getId());
            if (ventas == null || !evento.getId().equals(ventas.idEvento()) || ventas.entradasVendidas() == null) {
                throw new ServicioExternoException(
                        "Entrada devolvió una cantidad de ventas incompleta para el evento " + evento.getId(),
                        null
                );
            }
            return ventas.entradasVendidas();
        } catch (FeignException exception) {
            throw new ServicioExternoException(
                    "No se pudo consultar la cantidad de entradas vendidas en msvc-entrada",
                    exception
            );
        }
    }

    private boolean coincideParticipante(Evento evento, String criterio) {
        String normalizado = criterio.trim().toLowerCase(Locale.ROOT);
        return evento.getParticipantes().stream().map(relacion -> relacion.getParticipante())
                .anyMatch(participante -> participante.getNombre().toLowerCase(Locale.ROOT).contains(normalizado)
                        || participante.getId().toString().equals(criterio.trim()));
    }

    private boolean coincide(String valor, String criterio) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(criterio.trim().toLowerCase(Locale.ROOT));
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página debe ser mayor o igual a cero");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
    }

    private String normalizarOrden(String valor) {
        if (valor == null || valor.isBlank() || "fecha".equalsIgnoreCase(valor)) {
            return "fecha";
        }
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "precioasc", "precio_asc", "menorprecio" -> "precioasc";
            case "preciodesc", "precio_desc", "mayorprecio" -> "preciodesc";
            case "relevancia", "ventas" -> "relevancia";
            default -> throw new IllegalArgumentException("Orden no reconocido: " + valor);
        };
    }

    private boolean usaPrecio(String orden) {
        return "precioasc".equals(orden) || "preciodesc".equals(orden);
    }
}
