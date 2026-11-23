import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'

/** Detalle de un evento (lugar, sectores, participantes). Sirve para cualquier estado del evento. */
export const useEventDetail = (id: number) =>
  useQuery({
    queryKey: ['event-detail', id],
    queryFn: ({ signal }) => eventsApi.detail(id, signal),
    enabled: Number.isFinite(id),
  })
