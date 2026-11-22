import type { Session } from '../types'
import { decodeClaims } from './claims'

/** Arma la sesión a partir de un token. Devuelve null si el token es inválido o ya venció. */
export const buildSession = (token: string, email: string, now: number = Date.now()): Session | null => {
  const claims = decodeClaims(token)
  if (!claims) return null
  const expiresAt = claims.exp * 1000
  if (expiresAt <= now) return null
  return { token, email, idUsuario: claims.idUsuario, roles: claims.roles, expiresAt }
}
