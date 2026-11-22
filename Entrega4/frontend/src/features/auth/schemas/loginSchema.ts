import { z } from 'zod'
import { emailSchema } from './email'

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Ingresá tu contraseña.').max(72, 'La contraseña es demasiado larga.'),
})

export type LoginValues = z.infer<typeof loginSchema>
