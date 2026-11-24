import { queryOptions, useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'

/** Consulta del detalle de un evento; se comparte para que varias pantallas usen la misma caché. */
export const eventDetailQuery = (id: number) =>
  queryOptions({
    queryKey: ['event-detail', id],
    queryFn: ({ signal }) => eventsApi.detail(id, signal),
    enabled: Number.isFinite(id),
  })

/** Detalle de un evento (lugar, sectores, participantes). Sirve para cualquier estado del evento. */
export const useEventDetail = (id: number) => useQuery(eventDetailQuery(id))
