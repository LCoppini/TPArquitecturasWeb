package com.ticketya.usuarios.config;

import com.ticketya.usuarios.repository.RolRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RolSeeder implements ApplicationRunner {

    private static final List<String> ROLES_INICIALES = List.of(
            "CLIENTE", "ORGANIZADOR", "ADMINISTRADOR", "PERSONAL_ACCESO"
    );

    private final RolRepository rolRepository;

    public RolSeeder(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ROLES_INICIALES.forEach(rolRepository::insertarSiNoExiste);
    }
}
