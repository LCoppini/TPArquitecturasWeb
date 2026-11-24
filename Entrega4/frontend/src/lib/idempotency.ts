/** Clave única para operaciones que no deben duplicarse si se reintentan (compra, pago). */
export const newIdempotencyKey = (): string => crypto.randomUUID()
