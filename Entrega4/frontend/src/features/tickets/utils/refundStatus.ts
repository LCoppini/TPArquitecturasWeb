import type { TicketGroup } from './ticketStatus'

/** Estado de la devolución de una compra, deducido de sus entradas. */
export type RefundStatus =
  | { kind: 'pending'; cancelled: boolean }
  | { kind: 'rejected' }
  | { kind: 'approved'; amount: number; cancelled: boolean }

/**
 * Pendiente mientras haya una solicitud sin resolver o un reintegro en curso; luego rechazada y, si no, aprobada
 * (con lo reintegrado). `cancelled` distingue el reintegro automático por cancelación del evento.
 */
export const refundStatusOf = ({ tickets }: TicketGroup): RefundStatus | null => {
  const cancelled = tickets.some(({ motivoInvalidacion }) => motivoInvalidacion === 'CANCELACION')
  if (tickets.some(({ estado, estadoDevolucion }) => estadoDevolucion === 'PENDIENTE' || estado === 'EN_REINTEGRO')) {
    return { kind: 'pending', cancelled }
  }
  if (tickets.some(({ estadoDevolucion }) => estadoDevolucion === 'RECHAZADA')) return { kind: 'rejected' }
  const refunded = tickets.filter(({ estado }) => estado === 'DEVUELTA')
  if (refunded.length === 0) return null
  return { kind: 'approved', amount: refunded.reduce((total, { precio }) => total + precio, 0), cancelled }
}
