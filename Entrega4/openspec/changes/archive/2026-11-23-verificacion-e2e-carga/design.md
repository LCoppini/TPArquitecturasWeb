# Diseño: despliegue Docker y verificación manual (EP-21)

## Contexto

Los servicios hoy se arrancan uno por uno desde el IDE. El `docker-compose.yml`
solo define PostgreSQL y MongoDB. No hay perfil `docker`, por lo que los hosts
internos (`localhost`) y los tiempos de Eureka son los de desarrollo. Para
verificar dos instancias de `msvc-entrada` detrás del gateway hace falta un
entorno reproducible con un solo comando, healthchecks y descubrimiento por IP.

ADR-0002 ya fija compose para E2E; ADR-0003 fija el contrato de claves. **No se
crea un ADR nuevo.**

## Topología activa

| Servicio | Réplicas | Base | Healthcheck |
|---|---:|---|---|
| `postgres` | 1 | varias bases | `pg_isready` |
| `msvc-config` | 1 | — | `GET :8888/msvc-gateway/default` |
| `msvc-eureka` | 1 | — | `GET :8761/actuator/health` |
| `msvc-usuarios` | 1 | `ticketya_usuarios` | `GET :8081/actuator/health` |
| `msvc-ubicacion` | 1 | `ticketya_ubicacion` | `GET :8082/actuator/health` |
| `msvc-evento` | 1 | `ticketya_evento` | `GET :8083/actuator/health` |
| `msvc-entrada` | **2** | `ticketya_entrada` | `GET :8084/actuator/health` |
| `msvc-preferencias` | 1 | MongoDB `ticketya_preferencias` | `GET :8086/actuator/health` |
| `mongo` | 1 | — | healthcheck de la imagen |
| `msvc-gateway` | 1 | — | `GET :8080/actuator/health` |

`msvc-preferencias` y `mongo` se activan: el servicio ya está implementado
(`31d017c`). `msvc-notificaciones` queda como plantilla comentada porque hoy solo
existe el esqueleto. Los 9 Dockerfiles sí se crean, para cumplir la consigna de un
archivo por servicio, y ese módulo no se levanta todavía.

## Dockerfile por servicio

Un archivo físico por módulo: `msvc-config/Dockerfile`, `msvc-eureka/Dockerfile`,
`msvc-gateway/Dockerfile`, `msvc-usuarios/Dockerfile`, `msvc-ubicacion/Dockerfile`,
`msvc-evento/Dockerfile`, `msvc-entrada/Dockerfile`,
`msvc-notificaciones/Dockerfile` y `msvc-preferencias/Dockerfile`.

Patrón idéntico; solo cambia `msvc-XXXXX`:

```dockerfile
# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl msvc-XXXXX -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 appuser \
    && mkdir -p /app/logs /app/secrets \
    && chown -R appuser:appuser /app
WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/msvc-XXXXX/target/msvc-XXXXX-*.jar /app/app.jar
USER appuser
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- Contexto de build siempre la raíz de `Entrega4`.
- `WORKDIR /app` también en la imagen final: sin él, una ruta relativa de log
  caería en `/logs`, que el usuario no-root no puede crear.
- `curl` se instala solo para los healthchecks.
- Se descarta `ARG MODULE` / Dockerfile único parametrizado.

## Compose

Anclas reales en el archivo: `x-common-build` (solo `context: .`), `x-common-env`
(`SPRING_PROFILES_ACTIVE=docker`, `SPRING_CONFIG_IMPORT=configserver:http://msvc-config:8888`
sin `optional:`, `JAVA_TOOL_OPTIONS`) y `x-postgres-env` (`POSTGRES_HOST`, usuario
y contraseña con `${VAR:?mensaje}`).

Decisiones:

- **Postgres único** con `docker/postgres/init` creando una base por servicio;
  healthcheck con `pg_isready`, volumen con nombre y credenciales desde `.env`.
- **`msvc-config`**: `SPRING_PROFILES_ACTIVE=native,docker` para conservar
  `search-locations: classpath:/configuration`. Healthcheck en 8888. `mem_limit:
  512m` (si 384m da exit 137, ya está corregido). Recibe
  `LOGGING_FILE_NAME: /app/logs/msvc-config.log` porque no consume su propia
  configuración central.
- **`msvc-eureka`**: sin `EUREKA_INSTANCE_HOSTNAME` ni
  `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` por entorno; con `register-with-eureka:
  false` y `fetch-registry: false` no se usan. Healthcheck en **8761**, no en 8888.
  `mem_limit: 512m`.
- **Dominio**: healthcheck en `/actuator/health` del puerto propio;
  `depends_on` de `postgres` y `msvc-eureka` con `condition: service_healthy`.
  Ninguno publica puerto al host.
- **Preferencias**: `mongo` con volumen con nombre y healthcheck; el servicio
  recibe `MONGO_HOST=mongo` (su YAML ya usa `mongodb://${MONGO_HOST:localhost}`),
  depende de `mongo` y `msvc-eureka` y publica solo 8086 de forma interna.
- **`msvc-entrada`**: `deploy.replicas: 2` y `start_period: 30s`. Sin
  `container_name` y sin `--scale`. Los dos contenedores comparten
  `ticketya-logs`, pero sus archivos no se pisan por el `HOSTNAME` en el nombre.
- **`msvc-gateway`**: depende solo de `msvc-eureka` y `msvc-usuarios`. Si otro
  servicio aún no se registró, la ruta `lb://` responde 503; es esperable.
  Expone 8080. `JWT_PUBLIC_KEY_LOCATION` apunta al archivo montado `:ro`, con
  `chmod 644` en el host y `755` en el directorio para que lo lea el uid 10001.
- **Puertos**: 8888 y 8761 solo en `127.0.0.1`; 5432 se mantiene; 8080 público.
- **Secretos**: `JWT_PRIVATE_KEY` solo en `msvc-usuarios` con
  `${JWT_PRIVATE_KEY:?...}` (ADR-0003, sin cambios). `JWT_ISSUER`,
  `JWT_AUDIENCE` y credenciales también con `${VAR:?...}`. `.env` en
  `.gitignore` y `.dockerignore`; `.env.example` solo con placeholders.
- **`restart: unless-stopped`** en todos los servicios.
- `.dockerignore`: `.git`, `**/target`, `logs/`, `secrets/`, `.env`, `src/test`,
  IDEs y docs. `.gitattributes`: `*.sh text eol=lf` por Windows/autocrlf.

## Perfil `docker` en `msvc-config`

`configuration/application-docker.yml`, global para los clientes:

```yaml
eureka:
  client:
    registry-fetch-interval-seconds: 5
    service-url:
      defaultZone: http://msvc-eureka:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 5
    lease-expiration-duration-in-seconds: 15
    instance-id: ${spring.application.name}:${server.port}:${HOSTNAME:local}

spring:
  cloud:
    loadbalancer:
      cache:
        enabled: true
        ttl: 5s

logging:
  file:
    name: /app/logs/${spring.application.name}-${HOSTNAME:local}.log
```

En Spring se usa `${HOSTNAME:local}` y **no** `${HOSTNAME:-local}`: con `:-` el
valor por defecto quedaría `-local`. `msvc-config` toma el logging por
`LOGGING_FILE_NAME` en el compose, con nombre literal porque es instancia única.

`configuration/msvc-eureka-docker.yml`, solo el servidor:

```yaml
eureka:
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

El perfil global **no** define `register-with-eureka` ni `fetch-registry`: el
merge conserva `false/false` en `msvc-eureka`. La sección `management` con solo
`health` y `show-details: never` se declara en el YAML de cada servicio con
Actuator (gateway y dominio), no en el perfil global: así también es verificable
con el perfil `default`. `msvc-config` no tiene Actuator y no la recibe.

## Logging

Una sola property en el perfil docker, sin `logback-spring.xml` por servicio.
Cada contenedor escribe `/app/logs/<servicio>-<HOSTNAME>.log` en el volumen
compartido `ticketya-logs`. Las dos réplicas de entrada tienen `HOSTNAME`
distinto, así que no compiten por el mismo archivo. El patrón con
`correlationId` y el MDC quedan para EP-20.

## Memoria

| Escenario | Suma de `mem_limit` |
|---|---:|
| Compose activo (postgres + mongo + 8 servicios, entrada ×2) | **7680 MiB ≈ 7,5 GiB** |
| Si se activa además notificaciones | **8448 MiB ≈ 8,25 GiB** |

El consumo real se mide con `docker stats --no-stream` y se registra en
`verificacion-despliegue-docker.md`. En Windows, `.wslconfig` con
`memory=10GB` cubre ambos escenarios.

## Alternativas descartadas

- **Dockerfile único con `ARG MODULE`**: más corto, pero la consigna pide uno por
  servicio.
- **`logback-spring.xml` por servicio**: 7 archivos duplicados para una sola
  property; se resuelve con `logging.file.name` global.
- **`--scale msvc-entrada=2`**: convive mal con `deploy.replicas` en la misma
  documentación; se elige una sola forma.
- **`EUREKA_INSTANCE_HOSTNAME` fijo en los clientes**: las dos réplicas
  colisionarían en el registro; se usa `prefer-ip-address` e `instance-id` único.
- **Perfil docker que pise `register-with-eureka`/`fetch-registry`**: apagaría
  el registro del servidor Eureka.
- **Mongo activo antes**: el compose era inválido hasta que `msvc-preferencias`
  estuviera implementado; ya lo está (`31d017c`).
- **Activar notificaciones ya**: hoy solo hay esqueleto, así que se deja plantilla
  comentada para no romper el compose.
- **Testcontainers nuevos**: ADR-0002 exige compose para E2E.
- **ADR nuevo**: ADR-0002 ya cubre compose y ADR-0003 las claves.

## Pruebas y verificación

Tests de configuración (se agregan, no se relajan los existentes):

- `ConfiguracionCentralizadaTest`: `management...include=health` y
  `show-details=never`; los YAML del perfil docker se publican; el perfil global
  **no** cambia `register-with-eureka` ni `fetch-registry` del servidor.
- `GatewayRoleMatrixTest.cadaRutaDelGatewayTieneReglaExplicita`: cada predicado
  `Path` empieza con `/api/` y ninguno con `/internal/`.
- `JwtVerificacionRealTest`: un GET público con token vencido y otro firmado por
  otra clave responden 401 (`oauth2ResourceServer` procesa el header antes de
  `permitAll`).

Verificación manual (ejecutada el 2026-11-23 con login real): dos instancias
registradas, `GET /api/entradas` con JWT de `CLIENTE`, `docker stop` y
`docker kill` con tiempos medidos por separado, y `docker stats --no-stream`.
Durante la ventana de convergencia pueden aparecer errores hacia la instancia
caída; el retry de EP-13 los cubre. El procedimiento y la tabla de mediciones
viven en `verificacion-despliegue-docker.md`, dentro de este change.

## Consecuencias

- Se crean 9 Dockerfiles, `.dockerignore`, `.gitattributes`, el compose ampliado
  y los YAML del perfil docker. Nada de eso se escribe antes de la aprobación.
- `.env.example` gana los placeholders de Postgres; `.env` se ignora.
- Las variables de entorno son visibles con `docker inspect`: limitación
  conocida, en producción serían Docker secrets o un gestor de secretos. Se
  documenta en el README.
- El gateway mantiene su 503 para destinos no disponibles, distinto del 502 de
  circuito abierto de EP-13.
- JMeter se ejecutó dentro del mismo change: plan `prueba-carga.jmx` con 30
  clientes concurrentes sobre dos réplicas; resultados en `resultado-carga.md`.
  `maxPersonas` debe ser mayor que la concurrencia para que la tanda no se cierre
  antes de que los clientes compren.
