package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.dto.request.ActualizarUsuarioRequest;
import com.ticketya.usuarios.dto.request.CrearUsuarioRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.exception.SolicitudInvalidaException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.UsuarioService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private static final String UNIQUE_DNI = "uk_usuario_dni";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioMapper usuarioMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        validarDniNuevo(request.dni());
        Usuario usuario = new Usuario();
        usuario.setDni(request.dni());
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCiudad(request.ciudad().trim());
        usuario.setRoles(buscarRolesValidos(request.roles()));
        return guardar(usuario);
    }

    @Override
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByActivoTrueOrderByIdUsuarioAsc().stream()
                .map(usuarioMapper::aResponse)
                .toList();
    }

    @Override
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioMapper.aResponse(usuarioActivo(id));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioActivo(id);
        if (usuarioRepository.existsByDniAndIdUsuarioNot(request.dni(), id)) {
            throw new ConflictoException("El DNI ya está registrado");
        }
        usuario.setDni(request.dni());
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCiudad(request.ciudad().trim());
        return guardar(usuario);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioActivo(id);
        usuario.setActivo(false);
        usuarioRepository.saveAndFlush(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse reemplazarRoles(Long id, Set<String> tiposRoles) {
        Usuario usuario = usuarioActivo(id);
        usuario.setRoles(buscarRolesValidos(tiposRoles));
        return guardar(usuario);
    }

    private UsuarioResponse guardar(Usuario usuario) {
        try {
            return usuarioMapper.aResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            if (esConflictoDni(exception)) {
                throw new ConflictoException("El DNI ya está registrado");
            }
            throw exception;
        }
    }

    private boolean esConflictoDni(DataIntegrityViolationException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation) {
                return UNIQUE_DNI.equals(violation.getConstraintName());
            }
            causa = causa.getCause();
        }
        return false;
    }

    private void validarDniNuevo(Long dni) {
        if (usuarioRepository.existsByDni(dni)) {
            throw new ConflictoException("El DNI ya está registrado");
        }
    }

    private Set<Rol> buscarRolesValidos(Set<String> tiposRoles) {
        if (tiposRoles.isEmpty()) {
            return new HashSet<>();
        }
        List<Rol> roles = rolRepository.findAllByTipoIn(tiposRoles);
        Set<String> tiposEncontrados = roles.stream().map(Rol::getTipo).collect(Collectors.toSet());
        if (!tiposEncontrados.equals(tiposRoles)) {
            Set<String> desconocidos = tiposRoles.stream()
                    .filter(tipo -> !tiposEncontrados.contains(tipo))
                    .collect(Collectors.toSet());
            throw new SolicitudInvalidaException("Roles desconocidos: " + String.join(", ", desconocidos));
        }
        return new HashSet<>(roles);
    }

    private Usuario usuarioActivo(Long id) {
        return usuarioRepository.findByIdUsuarioAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario activo con id " + id));
    }
}
