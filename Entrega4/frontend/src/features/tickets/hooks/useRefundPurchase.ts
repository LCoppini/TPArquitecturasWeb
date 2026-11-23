import { useMutation, useQueryClient } from '@tanstack/react-query'
import { purchaseApi } from '@/features/purchase/api/purchaseApi'

/** Pide la devolución completa de una compra; el resultado llega de forma asíncrona, por eso se vuelve a consultar. */
export const useRefundPurchase = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (purchaseId: number) => purchaseApi.refund(purchaseId),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['my-tickets'] }),
  })
}
