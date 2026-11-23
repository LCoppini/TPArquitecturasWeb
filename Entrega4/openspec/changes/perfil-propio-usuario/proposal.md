# Proposal: perfil propio del usuario

## Por qué
El frontend necesita mostrar el perfil del usuario logueado (nombre, apellido, DNI, ciudad, email, rol). Hoy `GET /api/usuarios/{id}` es solo de ADMINISTRADOR en el gateway y `UsuarioResponse` no expone el email. Además msvc-notificaciones consulta el email a msvc-usuarios y hoy no lo recibe.

## Qué cambia
- msvc-usuarios: `GET /api/usuarios/me` toma el usuario de `X-User-Id` (derivado por el gateway del token) y devuelve su perfil.
- msvc-usuarios: `UsuarioResponse` suma `email` (constructor previo conservado).
- msvc-gateway: `GET /api/usuarios/me` queda permitido a cualquier usuario autenticado. El resto de `/api/usuarios/**` sigue siendo de ADMINISTRADOR.

## Fuera de alcance
- Edición del perfil propio.

## Riesgos
- Al exponer `email` en las respuestas de usuarios, los endpoints de ADMINISTRADOR también lo devuelven (es el mismo DTO).
