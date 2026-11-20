package com.ticketya.usuarios.service.impl;

import com.ticketya.usuarios.dto.request.LoginRequest;
import com.ticketya.usuarios.dto.response.LoginResponse;
import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.exception.CredencialesInvalidasException;
import com.ticketya.usuarios.repository.UsuarioRepository;
import com.ticketya.usuarios.service.AutenticacionService;
import com.ticketya.usuarios.service.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(CredencialesInvalidasException::new);

        if (usuario.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        Set<String> roles = usuario.getRoles().stream()
                .map(Rol::getTipo)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (roles.isEmpty()) {
            throw new CredencialesInvalidasException();
        }

        String token = tokenService.emitir(usuario.getIdUsuario(), roles);
        return new LoginResponse(token, "Bearer", tokenService.expiracionEnSegundos());
    }
}
