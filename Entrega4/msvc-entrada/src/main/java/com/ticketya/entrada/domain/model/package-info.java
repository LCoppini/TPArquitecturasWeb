/**
 * DOMINIO de msvc-entrada: Compra de entradas: reserva temporal, pago (mock), emision con QR, devoluciones y validacion de acceso. Aca vive el mecanismo anti doble venta.
 *
 * Aggregates / entidades / value objects / enums. Sin logica de infraestructura.
 * Las reglas de negocio viven ACA (metodos de las entidades), no en los controllers.
 * Nota: por pragmatismo las entidades llevan anotaciones JPA (@Entity). Si el grupo quiere DDD estricto, separar en infrastructure/persistence.
 *
 * Modelo esperado:
 *  - Entrada (id propio, id_evento, id_usuario, id_sector, [asiento], precio, codigo_qr, estado)
 *  -   - id_evento / id_usuario son REFERENCIAS por id (sin FK). El precio se guarda como snapshot al momento de la compra.
 *  -   - ATENCION: una PK compuesta (id_evento, id_usuario) impediria comprar 2 entradas para el mismo evento. Usar id propio.
 *  -   - Anti doble venta: restriccion UNIQUE (id_evento, id_sector, asiento) sobre entradas/reservas activas + clave de idempotencia.
 *  - Pago (id_pago, ...) y RegistroPago (id_entrada, id_pago, fecha_pago, monto_final) - detalle de compra y medio de pago
 *  - Reserva temporal (A DEFINIR por el grupo: entidad propia con expira_en, o estado RESERVADA en Entrada)
 *  - Devolucion (A DEFINIR: solicitud, estado PENDIENTE/APROBADA/RECHAZADA, ventana de 72 hs)
 */
package com.ticketya.entrada.domain.model;
