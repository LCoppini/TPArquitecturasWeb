# Proposal: productor de EVENTO_MODIFICADO

## Por qué
El tipo y la plantilla `EVENTO_MODIFICADO` (`evento`, `cambios`) existen en msvc-notificaciones y nadie los dispara. El punto natural es `OrganizadorEventoController.actualizar` en msvc-evento.

## Qué cambia
- msvc-evento: `actualizar` compara nombre, fecha y lugar antes y después. Si hubo cambios, tras el commit avisa a msvc-entrada por `POST /internal/entradas/eventos/{idEvento}/modificacion` con la descripción de los cambios.
- msvc-entrada: el endpoint interno publica `EventoModificadoEvent` con los usuarios que tienen entradas VALIDA (mismo camino que la cancelación) y un listener asíncrono envía `EVENTO_MODIFICADO` por usuario.
- Clave de idempotencia: `EVENTO_MODIFICADO:idEvento:<id>:idUsuario:<id>:<hash de cambios>`.

## Decisión
La regla de edición no cambia: msvc-evento solo permite editar en BORRADOR (hay un test que lo exige) y un borrador no tiene compradores. El productor queda cableado y probado, pero hoy no hay destinatarios reales. Si el grupo habilita editar eventos publicados, los avisos funcionan sin más cambios.

## Fuera de alcance
- Avisos por cambios de sectores o de estado.

## Riesgos
- Un fallo de msvc-entrada no revierte la edición: queda en el log.
