import { http } from '@/lib/http'
import type { LoginCredentials, LoginResponse, RegisterRequest } from '../types'

/** Llamadas del módulo de autenticación. Es el único lugar que conoce las rutas de msvc-usuarios. */
export const authApi = {
  login: (credentials: LoginCredentials) =>
    http<LoginResponse>('/usuarios/login', { method: 'POST', body: credentials }),

  register: (request: RegisterRequest) => http<unknown>('/usuarios/registro', { method: 'POST', body: request }),
}
