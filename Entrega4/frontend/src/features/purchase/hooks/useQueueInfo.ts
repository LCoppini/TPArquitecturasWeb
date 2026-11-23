import { useQuery } from '@tanstack/react-query'
import { queueApi } from '../api/queueApi'

/** Fila virtual del evento. `null` indica que el evento no tiene fila. */
export const useQueueInfo = (eventId: number, enabled: boolean) =>
  useQuery({ queryKey: ['queue-info', eventId], queryFn: () => queueApi.info(eventId), enabled })
