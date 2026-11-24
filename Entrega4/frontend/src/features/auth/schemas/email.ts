import { z } from 'zod'

/** Mismas reglas que el backend (@NotBlank @Email @Size(max = 255)), con mensajes en español. */
export const emailSchema = z
  .string()
  .trim()
  .min(1, 'Ingresá tu correo electrónico.')
  .max(255, 'El correo es demasiado largo.')
  .email('Ingresá un correo válido, por ejemplo nombre@correo.com.')
