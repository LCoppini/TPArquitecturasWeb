# Verificación manual del despliegue Docker (EP-21)

> Documento suelto del change `verificacion-e2e-carga`.
> Estado: **implementado, verificación manual pendiente**. El proposal fue
> aprobado por el grupo y los artefactos están escritos; falta ejecutar el
> procedimiento con un JWT real de `CLIENTE` y memoria suficiente.

## Precondiciones

- [x] Proposal, `design.md` y `tasks.md` aprobados por el grupo.
- [x] Dockerfiles, compose y perfil docker ya escritos.
      `docker compose config -q` pasa con variables de ejemplo.
- [ ] `POST /api/usuarios/login` devuelve un JWT válido de un usuario `CLIENTE`.
- [ ] `.env` completo (credenciales de Postgres, `JWT_PRIVATE_KEY`, `JWT_ISSUER`,
      `JWT_AUDIENCE`) y pública montada en `./secrets/`.
- [ ] Host con memoria suficiente: ~7,5 GiB teóricos para el compose activo;
      en Windows, `.wslconfig` con `memory=10GB`.

### Bloqueo registrado en esta ejecución

- Docker **29.7.2** y Compose **v5.4.0** están disponibles y la sintaxis del
  compose valida (`docker compose config -q`).
- `docker info` informa `MemTotal=3971616768` (~3,7 GiB) para el daemon actual,
  por debajo de los ~7,5 GiB de `mem_limit` del compose activo. Levantar las
  diez imágenes con las dos réplicas excedería la memoria disponible, así que
  **no se ejecutó `docker compose up`** ni se inventaron credenciales para
  forzarlo.
- Tampoco hay `.env` ni `secrets/` con un par RS256 real (ambos ignorados por
  Git), por lo que el login real de `CLIENTE` queda sin verificar.
- Antes de repetir la prueba: crear `%USERPROFILE%\.wslconfig` con
  `[wsl2]` / `memory=10GB`, ejecutar `wsl --shutdown`, completar `.env` y
  `secrets/ticketya_jwt_public.pem` y reiniciar Docker.

## Procedimiento

1. `docker compose up -d` (las 2 réplicas de `msvc-entrada` vienen en el compose;
   **no** usar `--scale`).
2. `curl http://127.0.0.1:8761/eureka/apps/MSVC-ENTRADA` → deben aparecer dos
   instancias.
3. Login real: `POST http://localhost:8080/api/usuarios/login` con un `CLIENTE`
   → guardar el Bearer.
4. Repetir `GET http://localhost:8080/api/entradas` con
   `Authorization: Bearer <token>` y confirmar respuestas desde ambas réplicas.
   No usar `GET /api/eventos` como sustituto.
5. `docker ps --filter name=msvc-entrada` → tomar un ID.
6. **Caso ordenado:** `docker stop <id>` → cronometrar hasta que Eureka quite la
   instancia y el gateway deje de enrutar a ella.
7. Reponer la réplica (`docker compose up -d msvc-entrada`) y esperar a que vuelva
   a figurar en Eureka.
8. **Caso abrupto:** `docker kill <id>` sobre la otra réplica → cronometrar por
   separado.
9. Confirmar que en `/eureka/apps/MSVC-ENTRADA` queda una sola instancia.
10. `docker stats --no-stream` → volcar la tabla de consumo real más abajo.
11. `docker compose logs -f msvc-entrada` y archivos de `ticketya-logs` como
    evidencia complementaria.

> Durante la ventana de convergencia puede haber errores hacia la instancia
> caída; el retry de EP-13 los cubre. Una vez convergido no deben aparecer fallos.
> No mezclar los tiempos de `stop` y `kill`: se registran por separado.

## Convergencia medida

| Caso | Comando | Tiempo medido | Evidencia |
|---|---|---:|---|
| Parada ordenada | `docker stop <id>` | — (pendiente) | — |
| Caída abrupta | `docker kill <id>` | — (pendiente) | — |

Referencia teórica (no sustituye a la medición): lease expiration 15 s + eviction
5 s + registry fetch 5 s + caché del LB 5 s ≈ 10-20 s.

## Consumo real (`docker stats --no-stream`)

| Contenedor | `mem_limit` | Consumo medido | Observaciones |
|---|---:|---:|---|
| postgres | 1g | — (pendiente) | |
| msvc-config | 512m | — (pendiente) | |
| msvc-eureka | 512m | — (pendiente) | |
| msvc-usuarios | 768m | — (pendiente) | |
| msvc-ubicacion | 768m | — (pendiente) | |
| msvc-evento | 768m | — (pendiente) | |
| msvc-entrada (réplica 1) | 768m | — (pendiente) | |
| msvc-entrada (réplica 2) | 768m | — (pendiente) | |
| mongo | 512m | — (pendiente) | |
| msvc-preferencias | 768m | — (pendiente) | |
| msvc-gateway | 512m | — (pendiente) | |
| **Total** | **7680 MiB ≈ 7,5 GiB** | — (pendiente) | |

Si se activa además notificaciones, el teórico sube a 8448 MiB ≈ 8,25 GiB.

## Resultado

- [ ] Las dos réplicas atendieron `GET /api/entradas` con JWT real de `CLIENTE`.
- [ ] `docker stop` y `docker kill` medidos por separado y registrados arriba.
- [ ] Consumo real registrado arriba.
- [ ] Sin errores después de la convergencia.

## Referencias

- `design.md` de este change.
- `tasks.md`, secciones 6 y 7.
- `docs/decisiones/0002-estrategia-pruebas-e2.md` (compose para E2E, sin
  Testcontainers nuevos).
- `docs/decisiones/0003-contrato-token-jwt.md` (clave privada por variable de
  entorno).
