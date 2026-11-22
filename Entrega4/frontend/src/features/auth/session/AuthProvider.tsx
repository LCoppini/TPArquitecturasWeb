import { useCallback, useLayoutEffect, useMemo, useState, type ReactNode } from 'react'
import { setAuthHandlers } from '@/lib/http'
import type { Session } from '../types'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { buildSession } from './buildSession'
import { clearStoredSession, restoreSession, storeSession } from './storage'
import { useSessionExpiry } from './useSessionExpiry'

/** Mantiene la sesión del cliente y le enseña al cliente HTTP cómo obtener el token y qué hacer ante un 401. */
export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [session, setSession] = useState<Session | null>(restoreSession)

  const signIn = useCallback((token: string, email: string) => {
    const next = buildSession(token, email)
    if (!next) throw new Error('El token recibido no es válido.')
    storeSession({ token, email })
    setSession(next)
    return next
  }, [])

  const signOut = useCallback(() => {
    clearStoredSession()
    setSession(null)
  }, [])

  useSessionExpiry(session, signOut)

  // Layout effect: el handler debe estar listo antes de que cualquier hijo dispare una consulta.
  useLayoutEffect(() => {
    setAuthHandlers({ getToken: () => session?.token ?? null, onUnauthorized: signOut })
  }, [session, signOut])

  const value = useMemo<AuthContextValue>(
    () => ({ session, isAuthenticated: session !== null, signIn, signOut }),
    [session, signIn, signOut],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
