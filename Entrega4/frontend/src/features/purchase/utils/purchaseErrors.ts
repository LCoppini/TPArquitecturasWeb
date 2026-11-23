import { ApiError } from '@/lib/http'

const BY_CODE: Record<string, string> = {
  COMPRA_PENDIENTE: 'Ya tenés una compra en curso para este evento. Terminá de pagarla o esperá a que venza.',
  CUPO_TANDA_INSUFICIENTE: 'No quedan suficientes entradas en tu turno. Probá con menos entradas.',
}

/** Mensaje legible para un error de reserva o pago, leyendo el código de negocio del cuerpo si lo hay. */
export const describePurchaseError = (error: unknown): string => {
  if (!(error instanceof ApiError)) return 'No pudimos completar la operación. Intentá de nuevo.'
  const code = (error.data as { codigo?: unknown } | undefined)?.codigo
  if (typeof code === 'string' && BY_CODE[code]) return BY_CODE[code]
  if (error.status === 422) return 'Esta operación ya se había intentado con otros datos. Volvé a empezar.'
  return error.message
}
