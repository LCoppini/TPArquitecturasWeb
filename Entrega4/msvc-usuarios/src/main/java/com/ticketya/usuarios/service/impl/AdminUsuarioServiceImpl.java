package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.dto.request.CrearUsuarioAdministrativoRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.SolicitudInvalidaException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.AdminUsuarioService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Alta administrativa con roles explicitos (ADR-0001). */
@Service
@Transactional
public class AdminUsuarioServiceImpl implements AdminUsuarioService {

    private static final String UNIQUE_EMAIL = "uk_usuario_email";
    private static final String UNIQUE_DNI = "uk_usuario_dni";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public AdminUsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper usuarioMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public UsuarioResponse crear(CrearUsuarioAdministrativoRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("El email ya está registrado");
        }
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictoException("El DNI ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setDni(request.dni());
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCiudad(request.ciudad().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRoles(buscarRolesValidos(request.roles()));

        try {
            return usuarioMapper.aResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            if (esConflicto(exception, UNIQUE_EMAIL)) {
                throw new ConflictoException("El email ya está registrado");
            }
            if (esConflicto(exception, UNIQUE_DNI)) {
                throw new ConflictoException("El DNI ya está registrado");
            }
            throw exception;
        }
    }

    private Set<Rol> buscarRolesValidos(Set<String> tiposRoles) {
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

    private boolean esConflicto(DataIntegrityViolationException exception, String restriccion) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation
                    && restriccion.equals(violation.getConstraintName())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
