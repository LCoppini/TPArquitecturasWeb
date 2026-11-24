# Proposal: aviso cuando un artista favorito publica un evento en tu ciudad

## Por qué
Es una de las notificaciones del enunciado: avisar al cliente cuando un artista que prefiere publica un evento en su ciudad. msvc-preferencias ya guarda artistas preferidos y ciudad por cliente (MongoDB).

## Qué cambia
| Servicio | Cambio |
|---|---|
| msvc-evento | Al publicar (BORRADOR o SUSPENDIDO a PUBLICADO) se emite `EventoPublicadoEvent` y, después del commit, un listener asíncrono avisa a msvc-notificaciones con evento, fecha, ciudad del lugar y artistas. |
| msvc-preferencias | Nuevo `GET /internal/preferencias/artistas/{idArtista}/clientes?ciudad=`: ids de clientes con ese artista y esa ciudad. |
| msvc-notificaciones | Dueño de la orquestación: tipo `ARTISTA_FAVORITO_PUBLICA_EVENTO`, plantilla, `PreferenciasClient`, endpoint interno `POST /internal/notificaciones/eventos-publicados`, deduplicación de usuarios entre artistas e idempotencia. |
| msvc-usuarios | Sin cambios (ya expone `email`). |

- Clave de idempotencia: `ARTISTA_FAVORITO_PUBLICA_EVENTO:idEvento:<id>:idUsuario:<id>`. Republicar tras una suspensión no repite el correo.
- Si un cliente sigue a varios artistas del mismo evento recibe un solo correo (menciona al primero).

## Fuera de alcance
- Preferencia de opt-out de avisos.
- Artistas sin id en preferencias.

## Riesgos
- Si msvc-notificaciones o msvc-preferencias no responden, la publicación del evento igual se completa: queda en el log.
- Alcance de ciudad: comparación sin distinguir mayúsculas ni acentos extremos (se normaliza con `trim` y mayúsculas/minúsculas).
