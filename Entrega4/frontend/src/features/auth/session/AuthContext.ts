import { createContext } from 'react'
import type { Session } from '../types'

export type AuthContextValue = {
  session: Session | null
  isAuthenticated: boolean
  /** true si la sesión se cerró porque el token venció (o el servidor lo rechazó), no porque la persona salió. */
  sessionExpired: boolean
  /** Guarda la sesión a partir de un token recién emitido. Lanza si el token no es válido. */
  signIn: (token: string, email: string) => Session
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
