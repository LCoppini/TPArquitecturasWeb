import { useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'

/** Entradas disponibles de un sector. Se refresca cada 15 s mientras la página está abierta. */
export const useSectorAvailability = (eventId: number, sectorId: number) =>
  useQuery({
    queryKey: ['availability', eventId, sectorId],
    queryFn: ({ signal }) => eventsApi.availability(eventId, sectorId, signal),
    refetchInterval: 15_000,
  })
