import { z } from 'zod'

export const citySchema = z.object({
  ciudad: z.string().trim().min(1, 'Ingresá tu ciudad.').max(255, 'La ciudad es demasiado larga.'),
})

export type CityValues = z.infer<typeof citySchema>
