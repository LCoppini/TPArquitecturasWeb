import type { EntradaApi } from '@/features/tickets/types'

/** Estados de la persona dentro de la fila virtual de un evento. */
export type QueueStatus = 'EN_ESPERA' | 'HABILITADO' | 'SALIO' | 'VENCIDO' | 'COMPRO' | 'CANCELADO'

/** Forma de FilaEventoResponse (GET /api/filas-virtuales/eventos/{id}). */
export type FilaApi = { idEvento: number; estado: string; maxPersonas: number; cupoEntradas: number }

/** Forma de IngresoFilaResponse. */
export type IngresoApi = {
  idEvento: number
  idIngreso: number
  secuencia: number
  estado: string
  posicion: number | null
  venceEn: string | null
  idTanda: number | null
  esperaEstimadaSegundos: number | null
}

/** Forma de CompraResponse. Con `codigo` es un rechazo de negocio (COMPRA_PENDIENTE, CUPO_TANDA_INSUFICIENTE). */
export type CompraApi = {
  idCompra: number | null
  idEvento: number | null
  estado: string | null
  total: number | null
  venceEn: string | null
  entradas: EntradaApi[]
  codigo: string | null
  cantidadDisponible: number | null
}

/** Forma de PagoCompraResponse: `estado` es el de la Compra (PAGADA, RECHAZADA, VENCIDA o RESERVADA si el pago demora). */
export type PagoApi = { idPago: number | null; idCompra: number; estado: string; total: number | null }

export type PurchaseSelection = { sectorId: number; sectorName: string; unitPrice: number; quantity: number }
