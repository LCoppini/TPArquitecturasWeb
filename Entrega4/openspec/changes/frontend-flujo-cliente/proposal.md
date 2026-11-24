# Proposal: flujo del cliente en el frontend

## Por qué
El TPE pide como mínimo buscar eventos, elegir sector, comprar y ver las entradas. El frontend solo tenía cartelera e ingreso.

## Qué cambia (Entrega4/frontend)
- Detalle de evento (`/eventos/:id`): sede, participantes, sectores con precio y entradas disponibles, para cualquier estado del evento. Sin mapa de asientos: el cliente elige sector y cantidad (el backend asigna los asientos).
- Compra: entrar a la fila virtual, ver lugar / personas delante / espera estimada, salir de la fila, reservar (`POST /api/compras`) y pagar, con cuenta regresiva y resultados PAGADA, RECHAZADA, VENCIDA y pago en demora.
- Mis entradas (`/mis-entradas`): entradas agrupadas por compra con código QR y devolución de la compra con confirmación.
- Perfil (`/perfil`) con `GET /api/usuarios/me`.
- Recomendados para vos en la cartelera (solo con sesión).
- Sesión vencida: aviso y pedido de ingresar de nuevo; las rutas privadas llevan al ingreso y vuelven donde estaba la persona.
- Dependencia nueva `qrcode.react` (ADR-0019).

## Dependencias de backend
`GET /api/eventos/{id}/detalle`, `GET /api/usuarios/me`, `GET`/`DELETE` de la fila (changes detalle-evento-cliente, perfil-propio-usuario, fila-salida-voluntaria, asignacion-automatica-asientos), y el seed con filas activas.

## Fuera de alcance
Flujo de organizador y administrador, mapa de asientos, edición de perfil.
