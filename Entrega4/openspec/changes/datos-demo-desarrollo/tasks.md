# Tasks

## 1. Seeds
- [x] 1.1 `DatosDemoUsuariosSeeder` en msvc-usuarios, con test de idempotencia y de roles.
- [x] 1.2 `DatosDemoUbicacionSeeder` en msvc-ubicacion, con test de capacidades y de idempotencia.
- [x] 1.3 `DatosDemoEventoSeeder` en msvc-evento, con test de estados, fechas futuras y organizador.

## 2. Verificación
- [ ] 2.1 Compilar y correr los tests de los tres servicios (`mvnw -pl msvc-usuarios,msvc-ubicacion,msvc-evento test`).
- [ ] 2.2 Con las bases vacías y `SPRING_PROFILES_ACTIVE=dev-seed`, levantar usuarios, ubicación y eventos, y comprobar en el log que cada seed cargó sus datos.
- [ ] 2.3 Verificar `GET /api/eventos` por el gateway y el ingreso con `cliente@ticketya.dev`.
- [ ] 2.4 Verificar que sin el perfil no se carga nada.

## 3. Documentación
- [ ] 3.1 Documentar en el README raíz cómo activar el seed y reiniciar las bases.
- [ ] 3.2 Verificar el alcance: `git status` solo muestra los tres seeders, sus tests y este cambio.

## Ampliación: msvc-entrada
- [x] `DatosDemoEntradaSeeder` (perfil dev-seed): fila ACTIVA en los eventos publicados 1 a 18 y dos compras PAGADAS del cliente (`seed.dev.id-cliente`, 3 por defecto) con QR
- [x] `DatosDemoEntradaSeederTest`
