import { ApiError, http } from '@/lib/http'
import type { CompraApi, PagoApi } from '../types'

export type ReserveInput = {
  eventId: number
  sectorId: number
  quantity: number
  idempotencyKey: string
}

export const purchaseApi = {
  /** Reserva `quantity` entradas del sector; el backend elige los asientos libres si el sector es numerado. */
  reserve: ({ eventId, sectorId, quantity, idempotencyKey }: ReserveInput) =>
    http<CompraApi>('/compras', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: { idEvento: eventId, entradas: Array.from({ length: quantity }, () => ({ idSector: sectorId })) },
    }),
  pay: async (purchaseId: number, paymentMethod: string, idempotencyKey: string): Promise<PagoApi> => {
    try {
      return await http<PagoApi>(`/compras/${purchaseId}/pago`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
        body: { medioDePago: paymentMethod },
      })
    } catch (error) {
      // Un pago sobre una Compra vencida responde 409 con el resultado en el cuerpo.
      const data = error instanceof ApiError ? (error.data as Partial<PagoApi> | undefined) : undefined
      if (data?.estado === 'VENCIDA') return data as PagoApi
      throw error
    }
  },
  get: (purchaseId: number) => http<CompraApi>(`/compras/${purchaseId}`),
  refund: (purchaseId: number) => http<void>(`/compras/${purchaseId}/devolucion`, { method: 'POST' }),
}
