package com.ticketya.usuarios.config;

import com.ticketya.usuarios.entity.Rol;
import com.ticketya.usuarios.entity.Usuario;
import com.ticketya.usuarios.repository.RolRepository;
import com.ticketya.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Crea el primer ADMINISTRADOR solo si se completan las variables de entorno (ADR-0003). */
@Component
public class AdministradorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorSeeder.class);
    private static final String ADMINISTRADOR = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String dni;
    private final String nombre;
    private final String apellido;
    private final String ciudad;

    public AdministradorSeeder(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${seed.admin.email:}") String email,
            @Value("${seed.admin.password:}") String password,
            @Value("${seed.admin.dni:}") String dni,
            @Value("${seed.admin.nombre:}") String nombre,
            @Value("${seed.admin.apellido:}") String apellido,
            @Value("${seed.admin.ciudad:}") String ciudad
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.ciudad = ciudad;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || dni.isBlank()) {
            log.info("Seed de administrador omitido: faltan variables SEED_ADMIN_*.");
            return;
        }

        String correo = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(correo)) {
            log.info("Seed de administrador omitido: ya existe un usuario con ese email.");
            return;
        }

        rolRepository.insertarSiNoExiste(ADMINISTRADOR);
        Rol administrador = rolRepository.findAllByTipoIn(Set.of(ADMINISTRADOR)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo cargar el rol ADMINISTRADOR."));

        Usuario usuario = new Usuario();
        usuario.setDni(parsearDni(dni));
        usuario.setNombre(valorObligatorio(nombre, "Admin"));
        usuario.setApellido(valorObligatorio(apellido, "TicketYa"));
        usuario.setCiudad(valorObligatorio(ciudad, "Tandil"));
        usuario.setEmail(correo);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRoles(new HashSet<>(Set.of(administrador)));
        usuarioRepository.saveAndFlush(usuario);
        log.info("Seed de administrador completado para {}.", correo);
    }

    private Long parsearDni(String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("SEED_ADMIN_DNI debe ser numérico.", exception);
        }
    }

    private String valorObligatorio(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }
}
