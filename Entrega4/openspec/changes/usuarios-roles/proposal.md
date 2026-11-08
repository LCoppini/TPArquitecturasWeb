# Proposal

## Why

`msvc-usuarios` todavía no ofrece operaciones para gestionar usuarios ni sus roles, aunque otros servicios necesitan consultar usuarios y la consigna requiere mantener esos datos en un servicio con persistencia propia. Esta capacidad establece un contrato REST consistente para el núcleo de Entrega 1, sin incorporar autenticación.

## What Changes

- Incorporar el alta, consulta, modificación y baja lógica de usuarios con DNI único, nombre, apellido y ciudad.
- Asociar a cada usuario uno o varios roles del catálogo persistido: `CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR` y `PERSONAL_ACCESO`.
- Inicializar el catálogo de roles y permitir asignarlos o quitarlos al gestionar usuarios; no se crearán ni eliminarán roles mediante la API.
- Exponer `GET /api/usuarios/{id}` para que otros servicios puedan consultar el usuario por su identificador, manteniendo compatibilidad con el cliente Feign existente en `msvc-entrada`.

**Fuera de alcance:** inicio de sesión, contraseñas, autenticación, JWT y autorización; creación o eliminación de roles; validación del QR y permisos del personal de acceso; notificaciones y funcionalidades exclusivas de Entrega 2.

## Capabilities

### New Capabilities
- `usuarios`: gestión y consulta de usuarios, datos de perfil y estado de baja lógica.
- `roles-usuario`: catálogo inicial de roles y asociación de uno o varios roles a usuarios.

### Modified Capabilities
- Ninguna.

## Impact

- **Servicio:** `msvc-usuarios`, su API REST, capa de negocio, persistencia PostgreSQL y pruebas.
- **Contrato:** `GET /api/usuarios/{id}` debe devolver una respuesta compatible con `msvc-entrada`; se definirán además endpoints CRUD y de asociación de roles.
- **Modelo:** tablas `usuario`, `rol` y `usuario_rol`; se incorporará estado para la baja lógica y unicidad de DNI.
- **Seguridad:** no se agregan dependencias ni autenticación; las operaciones no aplican autorización por rol en Entrega 1.
