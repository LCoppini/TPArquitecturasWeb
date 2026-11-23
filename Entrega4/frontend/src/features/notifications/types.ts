/** Forma de MiNotificacionResponse de msvc-notificaciones (GET /api/notificaciones/mias). */
export type NotificacionApi = {
  id: number
  /** COMPRA_CONFIRMADA, DEVOLUCION_APROBADA, EVENTO_CANCELADO, EVENTO_MODIFICADO o ARTISTA_FAVORITO_PUBLICA_EVENTO. */
  tipo: string
  asunto: string | null
  /** PENDIENTE, ENVIANDO, ENVIADA, FALLIDA o DESCARTADA. */
  estado: string
  fechaCreacion: string
  fechaEnvio: string | null
}
