# Despliegue Docker Specification

## Purpose

Define el empaquetado Docker y la verificación del despliegue completo: arranque
con compose, dos réplicas de `msvc-entrada`, secretos, logging compartido y
registro del consumo y de la prueba de carga.

## Requirements

### Requirement: Contenedor por microservicio [Entrega 2]

El sistema DEBE incluir un `Dockerfile` físico por cada uno de los 9 módulos
Spring Boot, con build multietapa, contexto en la raíz de `Entrega4`, `WORKDIR
/app` en la etapa final y ejecución bajo un usuario no-root.

#### Scenario: build de cada servicio

- Dado que los 9 módulos tienen `pom.xml` en el reactor
- Cuando se construye `msvc-<nombre>` desde la raíz con su Dockerfile
- Entonces se genera `msvc-<nombre>-*.jar` y se copia a `/app/app.jar`
- Y la compilación usa caché de Maven y no compila tests

#### Scenario: usuario no-root y directorio de trabajo

- Dado que la imagen final define `USER appuser` con uid 10001
- Cuando el contenedor escribe en `logs/`
- Entonces el archivo queda dentro de `/app/logs`
- Y el volumen compartido es escribible por ese uid

### Requirement: Arranque único con compose [Entrega 1 / ampliada en Entrega 2]

El sistema DEBE levantarse completo con un único `docker compose up -d`, con un
solo PostgreSQL y los servicios de aplicación, en el orden config → eureka →
dominio → gateway, usando healthchecks y `depends_on` con
`condition: service_healthy`.

#### Scenario: arranque desde cero

- Dado que no hay contenedores corriendo
- Cuando se ejecuta `docker compose up -d`
- Entonces PostgreSQL pasa `pg_isready` antes que los servicios de dominio
- Y `msvc-config` está sano antes que `msvc-eureka`
- Y el gateway queda disponible en 8080

#### Scenario: healthcheck de configuración y descubrimiento

- Dado que `msvc-config` no tiene starter de Actuator
- Cuando Compose evalúa su healthcheck
- Entonces consulta `GET http://localhost:8888/msvc-gateway/default`
- Y el healthcheck de `msvc-eureka` consulta `GET http://localhost:8761/actuator/health`

#### Scenario: dependencias mínimas del gateway

- Dado que los servicios nuevos todavía no se levantan
- Cuando arranca `msvc-gateway`
- Entonces solo depende de `msvc-eureka` y `msvc-usuarios`
- Y una ruta hacia un servicio aún no registrado responde 503, no falla el arranque

### Requirement: Dos instancias de msvc-entrada [Entrega 2]

`msvc-entrada` DEBE ejecutarse con dos réplicas definidas exclusivamente con
`deploy.replicas: 2`, sin `--scale`, y registrarse en Eureka con dirección IP e
`instance-id` único por contenedor.

#### Scenario: registro de las dos réplicas

- Dado que el compose declara `deploy.replicas: 2`
- Cuando se levanta el sistema
- Entonces aparecen dos instancias de `MSVC-ENTRADA` en Eureka
- Y cada `instance-id` termina en el `HOSTNAME` de su contenedor

#### Scenario: parada ordenada de una réplica

- Verificado el 2026-11-23 con un JWT real de `CLIENTE`; ver
  `verificacion-despliegue-docker.md`.
- Dado que las dos réplicas atienden `GET /api/entradas`
- Cuando se ejecuta `docker stop <id>` sobre una de ellas
- Entonces se mide y registra el tiempo hasta que Eureka la quita y el gateway
  deja de enrutar a ella
- Y se registra la evidencia en `verificacion-despliegue-docker.md`

#### Scenario: caída abrupta de una réplica

- Verificado el 2026-11-23 con un JWT real de `CLIENTE`; ver
  `verificacion-despliegue-docker.md`.
- Dado que la réplica detenida fue reemplazada y ambas vuelven a estar sanas
- Cuando se ejecuta `docker kill <id>` sobre una de ellas
- Entonces se mide por separado el tiempo de convergencia
- Y se registra ese segundo tiempo, sin reutilizar el del caso ordenado

#### Scenario: ventana de convergencia

- Dado que una réplica dejó de responder
- Cuando el gateway todavía la tiene en su caché de load balancing
- Entonces pueden aparecer errores hacia esa instancia durante la ventana
- Y una vez convergido no deben aparecer fallos, cubiertos por el retry de EP-13

### Requirement: Perfil docker centralizado [Entrega 2]

El perfil `docker` publicado por `msvc-config` DEBE fijar los hosts internos y los
tiempos rápidos de Eureka y del load balancer, sin modificar las banderas de
registro del servidor Eureka.

#### Scenario: hosts internos y tiempos

- Dado que un cliente arranca con `SPRING_PROFILES_ACTIVE=docker`
- Cuando consulta su configuración
- Entonces `defaultZone` es `http://msvc-eureka:8761/eureka/`
- Y `registry-fetch-interval-seconds` es 5, lease renewal 5, lease expiration 15
  y la caché del load balancer tiene TTL 5s

#### Scenario: el servidor sigue sin registrarse

- Dado que `msvc-eureka` arranca con el perfil docker
- Cuando se aplica el merge de configuración
- Entonces `register-with-eureka` y `fetch-registry` siguen en `false`
- Y `enable-self-preservation` es `false` con evición cada 5000 ms

#### Scenario: exposición de Actuator

- Dado que el gateway usa `securityMatcher("/api/**", "/internal/**")`
- Cuando se consulta `/actuator/health` sin token
- Entonces responde 200 sin pasar por la cadena de seguridad
- Y `show-details` es `never` en gateway y servicios con Actuator

### Requirement: Logging compartido [Entrega 2]

El sistema DEBE escribir los logs en un volumen compartido usando una única
property `logging.file.name` definida en el perfil docker global, con el nombre
de archivo diferenciado por `HOSTNAME`.

#### Scenario: archivo por contenedor

- Dado que dos réplicas de `msvc-entrada` escriben en `/app/logs`
- Cuando ambas inician
- Entonces generan `msvc-entrada-<HOSTNAME-1>.log` y `msvc-entrada-<HOSTNAME-2>.log`
- Y ninguna sobrescribe el archivo de la otra

#### Scenario: configuración del propio config server

- Dado que `msvc-config` no consume su configuración central
- Cuando arranca en el compose
- Entonces toma `LOGGING_FILE_NAME` del entorno
- Y escribe `/app/logs/msvc-config.log`

### Requirement: Secretos y variables obligatorias [Entrega 2]

El compose DEBE exigir las variables de entorno obligatorias con
`${VAR:?mensaje}` y entregar la clave privada únicamente a `msvc-usuarios`, sin
versionar `.env` ni claves.

#### Scenario: variable ausente

- Dado que `JWT_ISSUER` no está definida en `.env`
- Cuando se ejecuta `docker compose config`
- Entonces Compose falla con el mensaje indicado
- Y no se intenta arrancar ningún contenedor

#### Scenario: clave privada y clave pública

- Dado que ADR-0003 exige PEM de una línea por variable de entorno
- Cuando se levanta el sistema
- Entonces solo `msvc-usuarios` recibe `JWT_PRIVATE_KEY`
- Y el gateway monta la pública en `/run/secrets/` como solo lectura

### Requirement: Verificación manual y registro [Entrega 2]

El change DEBE documentar el procedimiento de verificación manual y registrar en
él los tiempos medidos de `docker stop` y `docker kill` y el consumo real de
`docker stats --no-stream`.

#### Scenario: llamada de verificación

- Verificado el 2026-11-23 con un JWT real de `CLIENTE`; ver
  `verificacion-despliegue-docker.md`.
- Dado que el gateway está disponible en 8080
- Cuando se llama a `GET /api/entradas` con `Authorization: Bearer <token>`
- Entonces las respuestas provienen de ambas réplicas de `msvc-entrada`
- Y no se usa `GET /api/eventos` como sustituto

#### Scenario: registro de consumo real

- Dado que el sistema está levantado con las 2 réplicas
- Cuando se ejecuta `docker stats --no-stream`
- Entonces se guarda el consumo medido por contenedor
- Y se documenta en `verificacion-despliegue-docker.md`, dentro de este change

### Requirement: Tests de configuración y seguridad [Entrega 2]

Los tests DEBEN verificar la exposición restringida de Actuator, que todas las
rutas del gateway estén bajo `/api/**`, y que un token inválido o vencido en una
ruta pública responda 401.

#### Scenario: rutas del gateway

- Dado que `application.yml` y `msvc-gateway.yml` declaran las rutas
- Cuando corre `cadaRutaDelGatewayTieneReglaExplicita`
- Entonces cada predicado `Path` empieza con `/api/`
- Y ninguno empieza con `/internal/`

#### Scenario: token inválido en ruta pública

- Dado que `GET /api/eventos` es pública
- Cuando se envía un token vencido o firmado con otra clave
- Entonces responde 401 con el cuerpo uniforme del gateway
- Y no se reenvía la petición al destino

#### Scenario: perfil docker sin banderas de servidor

- Dado que `application-docker.yml` se publica como configuración
- Cuando corre `ConfiguracionCentralizadaTest`
- Entonces no contiene `register-with-eureka` ni `fetch-registry`
- Y verifica `include=health` y `show-details=never`
