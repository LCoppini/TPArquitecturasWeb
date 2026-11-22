import { z } from 'zod'
import type { RegisterRequest } from '../types'
import { emailSchema } from './email'

const requiredText = (message: string) => z.string().trim().min(1, message).max(255, 'Usá como máximo 255 caracteres.')

/** Refleja RegistroRequest de msvc-usuarios: DNI positivo, textos obligatorios y contraseña de 8 a 72 caracteres. */
export const registerSchema = z.object({
  nombre: requiredText('Ingresá tu nombre.'),
  apellido: requiredText('Ingresá tu apellido.'),
  dni: z.string().trim().regex(/^\d{7,8}$/, 'Ingresá tu DNI sin puntos, de 7 u 8 dígitos.'),
  ciudad: requiredText('Ingresá tu ciudad.'),
  email: emailSchema,
  password: z.string().min(8, 'Usá al menos 8 caracteres.').max(72, 'Usá como máximo 72 caracteres.'),
  acceptTerms: z.boolean().refine((accepted) => accepted, 'Aceptá los términos y condiciones para continuar.'),
})

export type RegisterValues = z.infer<typeof registerSchema>

/** Convierte lo escrito en el formulario al cuerpo que espera el backend. */
export const toRegisterRequest = (values: RegisterValues): RegisterRequest => ({
  dni: Number(values.dni),
  nombre: values.nombre,
  apellido: values.apellido,
  ciudad: values.ciudad,
  email: values.email,
  password: values.password,
})
