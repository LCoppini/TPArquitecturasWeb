import { http } from '@/lib/http'
import type { NotificacionApi } from '../types'

export const notificationsApi = {
  /** Avisos enviados a la persona con sesión (hasta 100), del más nuevo al más viejo. */
  mine: (signal?: AbortSignal) => http<NotificacionApi[]>('/notificaciones/mias', { signal }),
}
