# Proposal: historial de notificaciones y ciudad propia para el cliente

## Por qué
El diseño del frontend incluye la pantalla "Notificaciones" (historial de avisos del cliente) y la edición de la ciudad en "Mi perfil". Hoy ninguna es posible desde el navegador: `msvc-notificaciones` solo tiene contrato interno (`/internal/**`, denegado en el gateway) y `PUT /api/usuarios/{id}` está reservado a ADMINISTRADOR en el gateway.

## Qué cambia
| Servicio | Cambio |
|---|---|
| msvc-notificaciones | Nuevo `GET /api/notificaciones/mias`: hasta 100 notificaciones del usuario autenticado (identidad por `X-User-Id`), la más reciente primero. Respuesta propia, sin datos internos (`ultimoError`, `claveIdempotencia`, `correlationId`). |
| msvc-usuarios | Nuevo `PUT /api/usuarios/me/ciudad` con `{ciudad}`: cambia solo la ciudad del usuario autenticado. |
| msvc-gateway | Ruta `notificaciones-route` (`/api/notificaciones/**`), regla de CLIENTE para ese prefijo y `PUT /api/usuarios/me/ciudad` para cualquier usuario autenticado. Se replica la ruta en `msvc-config`. |

## Fuera de alcance
- Reenviar una notificación fallida desde el cliente.
- Cambiar otros datos del perfil (DNI, nombre, correo) o preferencias de avisos.
- Ver notificaciones de otros usuarios.

## Riesgos
- El historial se limita a 100 filas; sin paginación en esta etapa.
- La ciudad del perfil no actualiza retroactivamente la ciudad de `msvc-preferencias` (se actualiza con la próxima compra).
