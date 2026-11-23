import type { EntradaApi } from '../types'

type Tone = 'success' | 'warning' | 'error' | 'info' | 'neutral'

export const TICKET_STATUS: Record<string, { label: string; tone: Tone }> = {
  RESERVADA: { label: 'Reservada', tone: 'warning' },
  VALIDA: { label: 'Válida', tone: 'success' },
  USADA: { label: 'Usada', tone: 'neutral' },
  VENCIDA: { label: 'Vencida', tone: 'neutral' },
  CANCELADA: { label: 'Cancelada', tone: 'error' },
  EN_REINTEGRO: { label: 'Devolución en curso', tone: 'info' },
  DEVUELTA: { label: 'Devuelta', tone: 'neutral' },
}

export const statusOf = (estado: string) => TICKET_STATUS[estado] ?? { label: estado, tone: 'neutral' as Tone }

export type TicketGroup = { key: string; purchaseId: number | null; tickets: EntradaApi[] }

/** Agrupa las entradas por compra, respetando el orden en que llegan (las más nuevas primero). */
export const groupByPurchase = (tickets: EntradaApi[]): TicketGroup[] => {
  const groups = new Map<string, TicketGroup>()
  for (const ticket of tickets) {
    const key = ticket.idCompra === null ? `solo-${ticket.id}` : `compra-${ticket.idCompra}`
    const group = groups.get(key) ?? { key, purchaseId: ticket.idCompra, tickets: [] }
    group.tickets.push(ticket)
    groups.set(key, group)
  }
  return [...groups.values()]
}

/** Se puede pedir la devolución si hay compra, alguna entrada sigue válida y ninguna está en devolución. */
export const canRefund = ({ purchaseId, tickets }: TicketGroup): boolean =>
  purchaseId !== null && tickets.some(({ estado }) => estado === 'VALIDA') && tickets.every(({ estadoDevolucion }) => estadoDevolucion === null)
