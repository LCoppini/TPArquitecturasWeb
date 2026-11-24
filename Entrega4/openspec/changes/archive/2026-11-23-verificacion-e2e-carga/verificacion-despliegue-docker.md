# Verificación manual del despliegue Docker (EP-21)

> Documento suelto del change `verificacion-e2e-carga`.
> Estado: **verificado con secretos locales de prueba**. El proposal fue
> aprobado por el grupo, los artefactos están escritos y el procedimiento se
> ejecutó con un JWT real de `CLIENTE`. Las credenciales utilizadas son locales
> y no están versionadas; en producción deben reemplazarse por las del entorno.
> La prueba de carga con JMeter también se ejecutó: ver `resultado-carga.md`.

## Precondiciones

- [x] Proposal, `design.md` y `tasks.md` aprobados por el grupo.
- [x] Dockerfiles, compose y perfil docker ya escritos.
      `docker compose config -q` pasa con variables de ejemplo.
- [x] `POST /api/usuarios/login` devuelve un JWT válido de un usuario `CLIENTE`
      (claims `idUsuario=1`, `roles=CLIENTE`, `iss=ticketya`, `aud=ticketya`).
- [x] `.env` completo (credenciales de Postgres, `JWT_PRIVATE_KEY`, `JWT_ISSUER`,
      `JWT_AUDIENCE`) y pública montada en `./secrets/`.
- [x] Host con memoria suficiente: `docker info` informa `MemTotal=8133427200`
      (~7,57 GiB) por encima de los ~7,5 GiB de `mem_limit` del compose activo.

### Resolución del bloqueo registrado antes

- Docker **29.7.2** y Compose **v5.4.0** están disponibles y la sintaxis del
  compose valida (`docker compose config -q`).
- En una ejecución anterior `docker info` informaba ~3,7 GiB y el compose no se
  había levantado. En esta ejecución el daemon dispone de ~7,57 GiB, por lo que
  `docker compose up -d --build` se ejecutó y levantó los 11 contenedores.
- Se generó un par RS256 **solo de prueba**: la privada quedó en `.env` y la
  pública en `secrets/ticketya_jwt_public.pem` (ambos ignorados por Git). Las
  credenciales de Postgres se reutilizaron del contenedor existente.
- El `CLIENTE` se creó por `POST /api/usuarios/registro` y el login se hizo a
  través del gateway en 8080.

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
| Parada ordenada | `docker stop entrega4-msvc-entrada-1` | **43,5 s** | Eureka quitó `msvc-entrada:8084:7d9ae92b42b6`; 3 errores transitorios durante la ventana y 20/20 respuestas 200 después de convergir. |
| Caída abrupta | `docker kill entrega4-msvc-entrada-2` | **50,3 s** | Eureka quitó `msvc-entrada:8084:cdc232c033d1`; 13 errores transitorios, 18/20 inmediatos y 30/30 respuestas 200 tras 15 s. |

Los tiempos incluyen el apagado graceful y los tiempos de Eureka/load balancer.
Referencia teórica (no sustituye a la medición): lease expiration 15 s + eviction
5 s + registry fetch 5 s + caché del LB 5 s ≈ 10-20 s.

La atribución de las llamadas a ambas réplicas (paso 4) se confirmó por el
incremento de tráfico de red en los dos contenedores (`docker stats`): ~28 kB de
RX en cada uno tras 40 peticiones. Los archivos de log por `HOSTNAME` quedan en
el volumen `ticketya-logs`, aunque no registran el path de la petición.

## Consumo real (`docker stats --no-stream`)

| Contenedor | `mem_limit` | Consumo medido | Observaciones |
|---|---:|---:|---|
| postgres | 1g | 130,4 MiB (12,74 %) | |
| msvc-config | 512m | 172,5 MiB (33,68 %) | |
| msvc-eureka | 512m | 272,3 MiB (53,18 %) | |
| msvc-usuarios | 768m | 361,3 MiB (47,04 %) | |
| msvc-ubicacion | 768m | 351,5 MiB (45,77 %) | |
| msvc-evento | 768m | 344 MiB (44,80 %) | |
| msvc-entrada (réplica 1) | 768m | 460,9 MiB (60,01 %) | |
| msvc-entrada (réplica 2) | 768m | 471,4 MiB (61,38 %) | |
| mongo | 512m | 97,21 MiB (18,99 %) | |
| msvc-preferencias | 768m | 277,4 MiB (36,12 %) | |
| msvc-gateway | 512m | 269,7 MiB (52,68 %) | |
| **Total** | **7680 MiB ≈ 7,5 GiB** | **~3208,6 MiB ≈ 3,13 GiB** | Medido con las dos réplicas sanas; se excluye el MySQL de la Entrega 1, que no pertenece a este compose. |

Si se activa además notificaciones, el teórico sube a 8448 MiB ≈ 8,25 GiB.

## Resultado

- [x] Las dos réplicas atendieron `GET /api/entradas` con JWT real de `CLIENTE`.
- [x] `docker stop` y `docker kill` medidos por separado y registrados arriba.
- [x] Consumo real registrado arriba.
- [x] Sin errores después de la convergencia (30/30 respuestas 200 tras 15 s).

Aparecieron errores transitorios durante la ventana de convergencia, cubiertos
por el retry de EP-13; una vez convergido el gateway volvió a responder 200.

## Referencias

- `resultado-carga.md` y `prueba-carga.jmx` de este change (sección 8).
- `design.md` de este change.
- `tasks.md`, secciones 6, 7 y 8.
- `docs/decisiones/0002-estrategia-pruebas-e2.md` (compose para E2E, sin
  Testcontainers nuevos).
- `docs/decisiones/0003-contrato-token-jwt.md` (clave privada por variable de
  entorno).
