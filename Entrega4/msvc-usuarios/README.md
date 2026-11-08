# msvc-usuarios

Servicio REST de usuarios y roles para TicketYa. En Entrega 1 no implementa autenticación ni autorización por rol.

## Ejecución local

1. Desde la raíz de `Entrega4`, iniciar PostgreSQL con `docker compose up -d`.
2. Iniciar `msvc-config` y `msvc-eureka`; luego iniciar `msvc-usuarios` desde IntelliJ o con Maven (`mvn -pl msvc-usuarios spring-boot:run`).
3. Swagger UI está disponible en `http://localhost:8081/swagger-ui.html`.

El servicio usa la base `ticketya_usuarios`. Al iniciar, asegura de forma idempotente el catálogo `CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR` y `PERSONAL_ACCESO`.

## Verificación con PostgreSQL

Con el contenedor de PostgreSQL iniciado, ejecutar `UsuarioPersistenciaIntegrationTest` desde IntelliJ para verificar el esquema, la inicialización idempotente del catálogo, la baja lógica con conservación de roles y la unicidad del DNI también para usuarios inactivos. La prueba está opt-in para no requerir PostgreSQL al ejecutar el resto de las pruebas: agregar `-Dusuarios.integration=true` a las opciones de VM de su configuración de test.

Si PostgreSQL informa `invalid value for parameter "TimeZone": "America/Buenos_Aires"`, agregar también `-Duser.timezone=UTC` a las opciones de VM de las configuraciones de ejecución y test de IntelliJ.

## API

- `POST /api/usuarios`: alta de usuario. Requiere `dni`, `nombre`, `apellido`, `ciudad` y `roles`.
- `GET /api/usuarios`: listado de usuarios activos.
- `GET /api/usuarios/{id}`: consulta por `id_usuario`; mantiene el campo de respuesta `idUsuario` que consume `msvc-entrada`.
- `PUT /api/usuarios/{id}`: actualización de perfil.
- `DELETE /api/usuarios/{id}`: baja lógica; conserva el usuario y las asociaciones de roles.
- `GET /api/roles`: catálogo disponible.
- `PUT /api/usuarios/{id}/roles`: reemplaza el conjunto de roles; una lista vacía los quita todos.

Las respuestas de error usan un JSON con `status`, `error`, `message` y `details`. Los códigos principales son 400 para solicitudes inválidas, 404 para usuarios inexistentes/inactivos y 409 para conflictos de DNI.
