import { http } from '@/lib/http'
import type { EntradaApi } from '../types'

export const ticketsApi = {
  /** Entradas de la persona con sesión, de la más nueva a la más vieja. */
  list: (signal?: AbortSignal) => http<EntradaApi[]>('/entradas', { signal }),
}
