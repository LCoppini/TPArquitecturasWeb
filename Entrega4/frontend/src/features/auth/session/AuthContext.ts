import { createContext } from 'react'
import type { Session } from '../types'

export type AuthContextValue = {
  session: Session | null
  isAuthenticated: boolean
  /** Guarda la sesión a partir de un token recién emitido. Lanza si el token no es válido. */
  signIn: (token: string, email: string) => Session
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
