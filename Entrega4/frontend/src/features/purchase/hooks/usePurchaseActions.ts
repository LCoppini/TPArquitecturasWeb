import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
import { purchaseApi } from '../api/purchaseApi'
import type { PurchaseSelection } from '../types'
import { createKeyBook } from '../utils/idempotencyKeys'

/** Reservar y pagar, con claves de idempotencia estables por operación (ver `createKeyBook`). */
export const usePurchaseActions = (eventId: number) => {
  const queryClient = useQueryClient()
  const reserveKeys = useRef(createKeyBook())
  const payKeys = useRef(createKeyBook())

  const reserve = useMutation({
    mutationFn: ({ sectorId, quantity }: PurchaseSelection) =>
      purchaseApi.reserve({
        eventId,
        sectorId,
        quantity,
        idempotencyKey: reserveKeys.current(`${eventId}:${sectorId}:${quantity}`),
      }),
  })
  const pay = useMutation({
    mutationFn: ({ purchaseId, method }: { purchaseId: number; method: string }) =>
      purchaseApi.pay(purchaseId, method, payKeys.current(`${purchaseId}:${method}`)),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['availability', eventId] })
      void queryClient.invalidateQueries({ queryKey: ['queue-entry', eventId] })
      void queryClient.invalidateQueries({ queryKey: ['my-tickets'] })
    },
  })
  return { reserve, pay }
}
