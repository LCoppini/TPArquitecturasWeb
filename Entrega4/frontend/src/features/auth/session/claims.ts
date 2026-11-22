import type { Role } from '../types'

export type TokenClaims = {
  idUsuario: number
  roles: Role[]
  /** Vencimiento en segundos desde epoch, como lo emite el backend. */
  exp: number
}

const decodeBase64Url = (value: string): string => {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const bytes = Uint8Array.from(atob(padded), (char) => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

const isClaims = (value: unknown): value is TokenClaims => {
  if (typeof value !== 'object' || value === null) return false
  const { idUsuario, roles, exp } = value as Record<string, unknown>
  return typeof idUsuario === 'number' && typeof exp === 'number' && Array.isArray(roles) && roles.every((r) => typeof r === 'string')
}

/**
 * Lee los claims del payload de un JWT. No verifica la firma: eso lo hace el gateway.
 * Sirve solo para decidir qué mostrar en pantalla. Devuelve null si el token está mal formado.
 */
export const decodeClaims = (token: string): TokenClaims | null => {
  const payload = token.split('.')[1]
  if (!payload) return null
  try {
    const claims: unknown = JSON.parse(decodeBase64Url(payload))
    return isClaims(claims) ? claims : null
  } catch {
    return null
  }
}
