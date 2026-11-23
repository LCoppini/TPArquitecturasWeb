# Propuesta: despliegue Docker y verificación manual (EP-21)

## Por qué

Hoy no existe ningún `Dockerfile` y el `docker-compose.yml` solo levanta PostgreSQL
y MongoDB: los servicios se arrancan a mano, en orden, desde el IDE. Eso impide
verificar en un entorno real dos hechos que el proyecto exige: que el sistema
arranque completo con un único `docker compose up`, y que `msvc-entrada` funcione
con al menos dos instancias detrás del gateway.

ADR-0002 ya decidió que las pruebas de integración E2E se hacen con
`docker-compose` y no con Testcontainers nuevos, así que este change es la
continuación natural de esa decisión. La verificación de reportes y la futura
verificación del TTL con dos instancias (EP-13) quedan apoyadas en este entorno.

## Qué cambia

- **Un Dockerfile físico por microservicio** (9 archivos): build multietapa con
  Maven, caché de dependencias, `WORKDIR /app` en ambas etapas, usuario no-root
  `appuser` (uid 10001) y `MaxRAMPercentage=70`. Se descartó un Dockerfile único
  parametrizado porque la consigna pide explícitamente uno por servicio.
- **`docker-compose.yml` ampliado**: un solo Postgres con varias bases, healthchecks,
  volúmenes con nombre, credenciales por `.env` con `${VAR:?mensaje}`, puertos de
  infraestructura solo en loopback, `mem_limit`, `restart: unless-stopped` y un
  volumen compartido de logs.
- **Perfil `docker` centralizado en `msvc-config`**: `defaultZone` interna, tiempos
  rápidos de Eureka y del load balancer, `prefer-ip-address` con `instance-id`
  basado en `${HOSTNAME:local}`, expose de solo `/actuator/health` y la property
  global de logging.
- **Logging en un solo lugar**: `logging.file.name` en el perfil docker global,
  sin `logback-spring.xml` por servicio. El patrón con `correlationId` queda
  pendiente de EP-20. `msvc-config`, que no consume su propia configuración,
  recibe `LOGGING_FILE_NAME` por variable de entorno en el compose.
- **Dos réplicas de `msvc-entrada`** con `deploy.replicas: 2` (sin `--scale`), con
  nombres de log distintos por contenedor y registro en Eureka por IP.
- **`msvc-preferencias` y MongoDB activos**: el servicio ya está implementado, así
  que se levantan en el compose con `MONGO_HOST=mongo` y su healthcheck.
- **Tests de configuración y límites**: exposición de solo `health`, rutas del
  gateway todas bajo `/api/**`, y 401 con token vencido/inválido en un GET público.
- **Procedimiento de verificación manual** con `docker stop` y `docker kill`
  medidos por separado, llamadas a `GET /api/entradas` con JWT de `CLIENTE` vía
  login real, y registro del consumo real con `docker stats --no-stream`.

## Capabilities

### Agregada

- `despliegue-docker`: empaquetado por servicio, arranque con compose, perfil
  docker, logging compartido y verificación manual del despliegue.

### Modificada

- Ninguna spec viva. El `docker-compose.yml` de Entrega 1 (solo bases) se amplía:
  antes levantaba PostgreSQL y MongoDB para ejecutar los servicios desde el IDE;
  ahora levanta también la aplicación y usa un único Postgres con varias bases.
  Se declara aquí para mantener la trazabilidad del cambio, aunque no exista una
  spec publicada de despliegue en `openspec/specs/`.

## Prerrequisitos verificados

- Los 9 módulos Spring Boot tienen `pom.xml` y están en el reactor (raíz).
- ADR-0003: la clave privada se entrega solo por `JWT_PRIVATE_KEY` a
  `msvc-usuarios`; la pública se monta como archivo en el gateway. No se crea ADR
  nuevo: ADR-0002 ya cubre el uso de `docker-compose` para E2E.
- `SecurityConfig` del gateway usa `securityMatcher("/api/**", "/internal/**")`,
  por lo que `/actuator/health` queda fuera de la cadena: no hace falta
  `permitAll` ni tocar la matriz de roles.
- `msvc-config` no tiene starter de Actuator; su healthcheck consulta
  `GET /msvc-gateway/default`. Eureka y Gateway sí exponen `/actuator/health`.
- No existe `logback-spring.xml` ni sección `logging:` en los YAML: por eso el
  logging se resuelve con una property en el perfil docker.
- El login real (`POST /api/usuarios/login`) era condición de la verificación
  manual; se verificó el 2026-11-23 con un JWT de `CLIENTE` y quedó registrado
  en `verificacion-despliegue-docker.md`.

## Aprobación antes de escribir

Este proposal, `design.md` y `tasks.md` deben ser revisados y aprobados por el
grupo. **No se escriben los Dockerfiles, el `docker-compose.yml`, los YAML de
configuración ni los tests hasta que esa aprobación exista.** Tampoco se hace push.

## Fuera de alcance

- Ejecución de JMeter e informe de carga: se completó dentro de este mismo
  change (sección 8); el plan y los resultados están en `prueba-carga.jmx` y
  `resultado-carga.md`.
- Activar `msvc-notificaciones` en el compose: hoy solo existe el esqueleto, así
  que sigue como plantilla comentada. `msvc-preferencias` y MongoDB sí se activan
  porque el servicio ya está implementado (commit `31d017c`).
- Patrón de log con `correlationId`, log centralizado y MDC: EP-20.
- Implementación de EP-13 (saga, idempotencia, TTL, circuit breaker) y de EP-19
  (caché): este change solo provee el entorno donde se verifican.
- Cambios en ADR-0003 (clave privada por variable de entorno) ni ADR nuevo.
- Dockerfile parametrizado único, `--scale`, Testcontainers nuevos, publicar los
  puertos de los servicios de dominio, o fijar `EUREKA_INSTANCE_HOSTNAME` en los
  clientes.
- Modificar tests existentes para que pasen, en particular `NoDobleVentaTest` y
  `QrAccesoConcurrenciaTest`.
