import type { NotificacionApi } from '../types'

export type NotificationFilter = 'all' | 'sent' | 'pending' | 'failed'
type Group = Exclude<NotificationFilter, 'all'>
type Tone = 'success' | 'info' | 'error'

export const NOTIFICATION_FILTERS: { id: NotificationFilter; label: string }[] = [
  { id: 'all', label: 'Todas' },
  { id: 'sent', label: 'Enviadas' },
  { id: 'pending', label: 'Pendientes' },
  { id: 'failed', label: 'Fallidas' },
]

const TYPE_LABELS: Record<string, string> = {
  COMPRA_CONFIRMADA: 'Compra confirmada',
  DEVOLUCION_APROBADA: 'Devolución aprobada',
  EVENTO_CANCELADO: 'Evento cancelado',
  EVENTO_MODIFICADO: 'Evento modificado',
  ARTISTA_FAVORITO_PUBLICA_EVENTO: 'Artista favorito',
}

/** Traduce el tipo y deja pasar los desconocidos. */
export const typeLabel = (tipo: string): string => TYPE_LABELS[tipo] ?? tipo

/** ENVIANDO cuenta como pendiente y DESCARTADA (sin correo o usuario) como fallida: ninguna llegó a la persona. */
export const groupOf = (estado: string): Group => {
  if (estado === 'ENVIADA') return 'sent'
  return estado === 'FALLIDA' || estado === 'DESCARTADA' ? 'failed' : 'pending'
}

const BADGES: Record<Group, { label: string; tone: Tone }> = {
  sent: { label: 'Enviada', tone: 'success' },
  pending: { label: 'Pendiente', tone: 'info' },
  failed: { label: 'Fallida', tone: 'error' },
}

export const badgeOf = (estado: string) => BADGES[groupOf(estado)]

export const filterNotifications = (items: NotificacionApi[], filter: NotificationFilter): NotificacionApi[] =>
  filter === 'all' ? items : items.filter(({ estado }) => groupOf(estado) === filter)

export const countNotifications = (items: NotificacionApi[]): Record<NotificationFilter, number> => {
  const counts: Record<NotificationFilter, number> = { all: items.length, sent: 0, pending: 0, failed: 0 }
  for (const { estado } of items) counts[groupOf(estado)] += 1
  return counts
}
