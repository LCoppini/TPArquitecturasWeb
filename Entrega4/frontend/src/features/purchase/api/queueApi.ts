import { ApiError, http } from '@/lib/http'
import type { FilaApi, IngresoApi } from '../types'

const base = (eventId: number) => `/filas-virtuales/eventos/${eventId}`

/** El backend responde 404 cuando no hay fila o no hay ingreso; para la vista eso es "ninguno", no un error. */
const orNull = async <T>(request: Promise<T>): Promise<T | null> => {
  try {
    return await request
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export const queueApi = {
  info: (eventId: number) => orNull(http<FilaApi>(base(eventId))),
  myEntry: (eventId: number) => orNull(http<IngresoApi>(`${base(eventId)}/mi-estado`)),
  join: (eventId: number) => http<IngresoApi>(`${base(eventId)}/ingresos`, { method: 'POST' }),
  leave: (eventId: number) => http<IngresoApi>(`${base(eventId)}/ingresos/mi`, { method: 'DELETE' }),
}
