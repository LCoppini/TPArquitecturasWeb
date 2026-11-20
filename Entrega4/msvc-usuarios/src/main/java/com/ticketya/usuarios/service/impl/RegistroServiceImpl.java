package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.dto.request.RegistroRequest;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.SolicitudInvalidaException;
import com.ticketya.usuarios.mapper.UsuarioMapper;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.RegistroService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RegistroServiceImpl implements RegistroService {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String UNIQUE_EMAIL = "uk_usuario_email";
    private static final String UNIQUE_DNI = "uk_usuario_dni";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public RegistroServiceImpl(
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
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("El email ya está registrado");
        }
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictoException("El DNI ya está registrado");
        }

        Rol cliente = rolRepository.findAllByTipoIn(Set.of(ROL_CLIENTE)).stream()
                .findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException("No existe el rol CLIENTE."));

        Usuario usuario = new Usuario();
        usuario.setDni(request.dni());
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setCiudad(request.ciudad().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRoles(new HashSet<>(Set.of(cliente)));

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

    private boolean esConflicto(DataIntegrityViolationException exception, String restraint) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation
                    && restraint.equals(violation.getConstraintName())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
