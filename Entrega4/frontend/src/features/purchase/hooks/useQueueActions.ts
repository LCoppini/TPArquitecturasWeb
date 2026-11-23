import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queueApi } from '../api/queueApi'

/** Entrar y salir de la fila: ambos dejan el resultado en la caché de "mi lugar". */
export const useQueueActions = (eventId: number) => {
  const queryClient = useQueryClient()
  const key = ['queue-entry', eventId]
  const join = useMutation({
    mutationFn: () => queueApi.join(eventId),
    onSuccess: (entry) => queryClient.setQueryData(key, entry),
  })
  const leave = useMutation({
    mutationFn: () => queueApi.leave(eventId),
    onSuccess: (entry) => queryClient.setQueryData(key, entry),
  })
  return { join, leave }
}
