import { newIdempotencyKey } from '@/lib/idempotency'

/**
 * Devuelve siempre la misma clave para la misma operación (misma firma) y una nueva si cambian los datos.
 * Así un reintento por falla de red no duplica la compra, y un pedido distinto no choca con el anterior.
 */
export const createKeyBook = () => {
  const keys = new Map<string, string>()
  return (signature: string): string => {
    const existing = keys.get(signature)
    if (existing) return existing
    const created = newIdempotencyKey()
    keys.set(signature, created)
    return created
  }
}
