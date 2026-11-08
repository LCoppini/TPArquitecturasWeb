package com.ticketya.usuarios.config;

import com.ticketya.usuarios.repository.RolRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RolSeederTest {

    @Test
    void solicitaLaInsercionIdempotenteDeLosCuatroRolesEnCadaInicio() throws Exception {
        RolRepository repository = mock(RolRepository.class);
        RolSeeder seeder = new RolSeeder(repository);

        seeder.run(null);
        seeder.run(null);

        verify(repository, times(2)).insertarSiNoExiste("CLIENTE");
        verify(repository, times(2)).insertarSiNoExiste("ORGANIZADOR");
        verify(repository, times(2)).insertarSiNoExiste("ADMINISTRADOR");
        verify(repository, times(2)).insertarSiNoExiste("PERSONAL_ACCESO");
    }
}
