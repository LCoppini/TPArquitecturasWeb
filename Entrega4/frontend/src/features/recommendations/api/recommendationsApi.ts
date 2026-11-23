import { http } from '@/lib/http'

/** Forma de RecomendacionResponse de msvc-preferencias. */
export type RecomendacionApi = { idEvento: number; fechaHora: string; tipo: string; motivo: string }

export const recommendationsApi = {
  forClient: (userId: number, signal?: AbortSignal) =>
    http<RecomendacionApi[]>(`/recomendaciones/clientes/${userId}`, { signal }),
}
