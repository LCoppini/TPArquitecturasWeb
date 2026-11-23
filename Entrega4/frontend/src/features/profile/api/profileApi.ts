import { http } from '@/lib/http'
import type { Role } from '@/features/auth/types'

/** Forma de UsuarioResponse de msvc-usuarios (GET /api/usuarios/me). */
export type UsuarioApi = {
  idUsuario: number
  dni: number
  nombre: string
  apellido: string
  ciudad: string
  email: string | null
  roles: { idRol: number; tipo: Role }[]
}

export const profileApi = {
  me: (signal?: AbortSignal) => http<UsuarioApi>('/usuarios/me', { signal }),
}
