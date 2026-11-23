import { useQuery } from '@tanstack/react-query'
import { queueApi } from '../api/queueApi'

const POLL_MS = 4_000

/** Mi lugar en la fila. Mientras espero o estoy habilitado se consulta cada pocos segundos. */
export const useMyQueueEntry = (eventId: number, enabled: boolean) =>
  useQuery({
    queryKey: ['queue-entry', eventId],
    queryFn: () => queueApi.myEntry(eventId),
    enabled,
    refetchInterval: ({ state }) =>
      state.data?.estado === 'EN_ESPERA' || state.data?.estado === 'HABILITADO' ? POLL_MS : false,
  })
