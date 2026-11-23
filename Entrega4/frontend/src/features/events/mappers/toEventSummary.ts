import { categoryLabelOf } from '../constants/categories'
import type { EventoApi, EventoPaginaApi, EventPage, EventSaleStatus, EventSummary } from '../types/event'

const STATUS_BY_ESTADO: Record<string, EventSaleStatus> = {
  AGOTADO: 'soldout',
  CANCELADO: 'cancelled',
  SUSPENDIDO: 'suspended',
  FINALIZADO: 'finished',
}

/** Estado del backend -> estado de venta de la vista. PUBLICADO (y cualquier otro) se considera a la venta. */
export const toSaleStatus = (estado: string): EventSaleStatus => STATUS_BY_ESTADO[estado] ?? 'available'

const lowestPrice = (sectores: EventoApi['sectores']): number | null =>
  sectores.length === 0 ? null : Math.min(...sectores.map(({ precio }) => precio))

export const toEventSummary = (evento: EventoApi): EventSummary => ({
  id: evento.id,
  title: evento.nombre,
  category: categoryLabelOf(evento.tipo),
  startsAt: evento.fechaHora,
  fromPrice: lowestPrice(evento.sectores),
  status: toSaleStatus(evento.estado),
})

export const toEventPage = (pagina: EventoPaginaApi): EventPage => ({
  items: pagina.contenido.map(toEventSummary),
  page: pagina.pagina + 1,
  totalPages: pagina.totalPaginas,
  totalItems: pagina.totalElementos,
})
