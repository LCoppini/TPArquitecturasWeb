import { http } from '@/lib/http'
import { toEventPage } from '../mappers/toEventSummary'
import type { EventoPaginaApi, EventPage } from '../types/event'

export type ListEventsParams = {
  /** Valor de `tipo` del backend; sin valor trae todas las categorías. */
  tipo?: string
  /** Página base 1. */
  page: number
  size: number
  signal?: AbortSignal
}

/** Llamadas de consulta del catálogo. Es el único lugar que conoce las rutas de msvc-evento. */
export const eventsApi = {
  list: async ({ tipo, page, size, signal }: ListEventsParams): Promise<EventPage> =>
    toEventPage(
      await http<EventoPaginaApi>('/eventos', {
        query: { tipo, pagina: page - 1, tamano: size, orden: 'fecha' },
        signal,
      }),
    ),
}
