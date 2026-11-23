# Tasks

## 0. Aprobación del grupo (bloqueante)

- [x] 0.1 Revisar `proposal.md`, `specs/despliegue-docker/spec.md`, `design.md` y
      este `tasks.md` con el grupo.
- [x] 0.2 Ejecutar `openspec validate verificacion-e2e-carga` y corregir lo que
      indique la CLI.
- [x] 0.3 **No escribir ningún archivo de Docker** (Dockerfiles, compose, YAML del
      perfil, `.dockerignore`, `.gitattributes`, README ni tests) hasta que 0.1
      esté aprobado. Tampoco hacer push.

## 1. Dockerfiles (uno por servicio)

- [x] 1.1 Crear los 9 Dockerfiles: `msvc-config`, `msvc-eureka`, `msvc-gateway`,
      `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento`, `msvc-entrada`,
      `msvc-notificaciones` y `msvc-preferencias`.
- [x] 1.2 En cada uno: build multietapa con Maven, caché `--mount=type=cache`,
      `-pl msvc-<nombre> -am package -Dmaven.test.skip=true`, contexto en la raíz.
- [x] 1.3 En la etapa final: `curl`, usuario `appuser` uid 10001, `WORKDIR /app`
      antes de `USER appuser`, `JAVA_TOOL_OPTIONS` con
      `-XX:MaxRAMPercentage=70 -Duser.timezone=UTC`.
- [x] 1.4 Crear `.dockerignore` con `.git`, `**/target`, `logs/`, `secrets/`,
      `.env`, `**/src/test`, IDEs y docs. Crear `.gitattributes` con
      `*.sh text eol=lf`.

## 2. docker-compose.yml

- [x] 2.1 Definir los anclas reales `x-common-build`, `x-common-env` y
      `x-postgres-env` con `${VAR:?mensaje}` para credenciales y variables JWT.
- [x] 2.2 Configurar `postgres` único con `pg_isready`, volumen con nombre,
      `docker/postgres/init` y `mem_limit: 1g`.
- [x] 2.3 Configurar `msvc-config` con `native,docker`, healthcheck en 8888,
      `mem_limit: 512m` y `LOGGING_FILE_NAME` literal.
- [x] 2.4 Configurar `msvc-eureka` sin variables de hostname ni `defaultZone` por
      entorno, healthcheck en **8761**, `mem_limit: 512m` y `SPRING_CONFIG_IMPORT`
      sin `optional:`.
- [x] 2.5 Configurar `msvc-usuarios`, `msvc-ubicacion`, `msvc-evento`,
      `msvc-entrada` y `msvc-gateway` con healthcheck en sus puertos, volumen de
      logs y `restart: unless-stopped`.
- [x] 2.6 En `msvc-entrada` usar solo `deploy.replicas: 2`, sin `container_name`
      ni `--scale`, con `start_period: 30s`.
- [x] 2.7 En `msvc-gateway`: exponer 8080, depender solo de `msvc-eureka` y
      `msvc-usuarios`, montar la pública `:ro` y exigir `JWT_ISSUER`,
      `JWT_AUDIENCE` y `JWT_PUBLIC_KEY_LOCATION`.
- [x] 2.8 Publicar 8888 y 8761 solo en `127.0.0.1`; no exponer puertos de dominio.
- [x] 2.9 Activar `mongo` con volumen con nombre y healthcheck, y
      `msvc-preferencias` en 8086 con `MONGO_HOST=mongo` y dependencia de `mongo`
      y `msvc-eureka`.
- [x] 2.10 Dejar como plantilla comentada solo `msvc-notificaciones` (hoy es
      esqueleto).

## 3. Perfil docker en msvc-config

- [x] 3.1 Crear `configuration/application-docker.yml` con `defaultZone` interna,
      `prefer-ip-address: true`, `instance-id` con `${HOSTNAME:local}`,
      registry fetch 5s, lease renewal 5s, lease expiration 15s, caché del LB 5s
      y `logging.file.name`.
- [x] 3.2 Crear `configuration/msvc-eureka-docker.yml` con
      `enable-self-preservation: false` y `eviction-interval-timer-in-ms: 5000`.
- [x] 3.3 Verificar que el perfil global NO defina `register-with-eureka` ni
      `fetch-registry`.
- [x] 3.4 Agregar `management...health` a los YAML de gateway, eureka y servicios
      de dominio; `msvc-config` queda sin Actuator.

## 4. Seguridad y secretos

- [x] 4.1 Agregar `.env` a `.gitignore` y completar `.env.example` solo con
      placeholders, incluyendo `POSTGRES_USER` y `POSTGRES_PASSWORD`.
- [x] 4.2 Verificar que `JWT_PRIVATE_KEY` aparece solo en `msvc-usuarios` y que
      ADR-0003 no se modifica.
- [x] 4.3 Documentar permisos de la pública en el host (`644` archivo, `755`
      directorio) para el uid 10001.
- [x] 4.4 Documentar en el README que las variables se ven con `docker inspect`
      (en producción serían Docker secrets o un gestor) y el `.wslconfig` con
      `memory=10GB`.

## 5. Tests de configuración y seguridad (se agregan, no se relajan)

- [x] 5.1 `ConfiguracionCentralizadaTest`: `include=health`, `show-details=never`,
      perfiles docker publicados y ausencia de `register-with-eureka` /
      `fetch-registry` en el perfil global.
- [x] 5.2 `GatewayRoleMatrixTest.cadaRutaDelGatewayTieneReglaExplicita`: cada
      `Path` empieza con `/api/` y ninguno con `/internal/`.
- [x] 5.3 `JwtVerificacionRealTest`: GET público con token vencido y con token de
      otra clave responde 401 sin llamar al destino.
- [x] 5.4 No modificar `NoDobleVentaTest` ni `QrAccesoConcurrenciaTest`.

## 6. Verificación manual (bloqueada por login real)

- [ ] 6.0 **Prerrequisito:** `POST /api/usuarios/login` devuelve un JWT válido de
      un usuario `CLIENTE`. Sin esto no se ejecutan 6.1 a 6.6.
- [ ] 6.1 Ejecutar `docker compose up -d` y confirmar dos instancias de
      `MSVC-ENTRADA` en `http://127.0.0.1:8761`.
- [ ] 6.2 Obtener el token de `CLIENTE` con login real.
- [ ] 6.3 Llamar repetidamente a `GET /api/entradas` por el gateway con el Bearer
      y confirmar respuestas desde ambas réplicas.
- [ ] 6.4 `docker stop <id>`: medir y registrar el tiempo hasta que Eureka quita
      la instancia y el gateway deja de enrutar a ella.
- [ ] 6.5 Reponer la réplica y `docker kill <id>`: medir y registrar ese segundo
      tiempo por separado.
- [ ] 6.6 Verificar en `/eureka/apps/MSVC-ENTRADA` que queda una sola instancia y
      anotar que durante la ventana pueden aparecer errores cubiertos por el
      retry de EP-13.

## 7. Registro de consumo y documentación

- [ ] 7.1 Ejecutar `docker stats --no-stream` con las dos réplicas levantadas.
- [ ] 7.2 Completar la tabla de consumo real en
      `verificacion-despliegue-docker.md` (archivo suelto de este change).
- [ ] 7.3 Completar las tablas de tiempos medidos de `docker stop` y `docker kill`.
- [x] 7.4 Documentar en el README cómo leer los logs del volumen `ticketya-logs`
      y cómo filtrar por archivo de contenedor.

## 8. JMeter (tareas posteriores del mismo change)

- [ ] 8.1 Definir el plan de pruebas del flujo de compra con al menos 2 instancias
      de `msvc-entrada`.
- [ ] 8.2 Ejecutar la prueba de carga y generar el informe de usuarios
      concurrentes, tiempos de respuesta y errores.
- [ ] 8.3 Registrar resultados y conclusiones dentro de este change.

## 9. Cierre

- [x] 9.1 Ejecutar `mvn -B package` y `openspec validate verificacion-e2e-carga`.
- [x] 9.2 No hacer push: queda pendiente del acuerdo del grupo.
- [ ] 9.3 Al archivar, mover la carpeta a `openspec/changes/archive/` sin agregar
      `archived:` a `.openspec.yaml`, igual que los changes anteriores.
