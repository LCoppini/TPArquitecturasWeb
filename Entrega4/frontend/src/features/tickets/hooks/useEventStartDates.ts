import { useQueries } from '@tanstack/react-query'
import { eventDetailQuery } from '@/features/events/hooks/useEventDetail'
import type { StartDates } from '../utils/ticketTabs'

/** Fecha de inicio de cada evento de las entradas; reutiliza la caché del detalle que ya consulta cada tarjeta. */
export const useEventStartDates = (eventIds: number[]): StartDates => {
  const results = useQueries({ queries: eventIds.map(eventDetailQuery) })
  const dates = new Map<number, string>()
  results.forEach(({ data }, index) => {
    if (data) dates.set(eventIds[index], data.startsAt)
  })
  return dates
}
