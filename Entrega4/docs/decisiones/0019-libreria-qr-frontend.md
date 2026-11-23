# ADR-0019: Librería para dibujar el código QR de las entradas en el frontend

- Estado: aceptada
- Fecha: 2026-11-23

## Contexto

msvc-entrada emite para cada entrada pagada un `codigoQr` en texto (un UUID). Para que la persona pueda mostrarlo en la puerta, el frontend debe dibujarlo como imagen QR escaneable. Hace falta una dependencia nueva en `Entrega4/frontend`.

## Decisión

Se incorpora `qrcode.react` (licencia ISC) y se usa solo su componente `QRCodeSVG` dentro de `TicketQr`. Genera el QR en el navegador a partir del texto, sin llamadas de red y sin enviar el código a terceros. Es un componente de React con soporte de TypeScript, sin dependencias de runtime adicionales.

## Alternativas descartadas

- Servicio externo que devuelve la imagen: filtra los códigos de acceso a un tercero y agrega una dependencia de red.
- Generar el QR en el backend: exigiría un endpoint nuevo y transportar imágenes por el gateway para algo que el cliente resuelve con el texto.
- `qrcode` (canvas/PNG): sirve, pero el SVG escala sin pérdida y es accesible con `role="img"`.

## Consecuencias

Una dependencia más en el frontend. Si el contrato del `codigoQr` cambia de formato, solo se modifica `TicketQr`.
