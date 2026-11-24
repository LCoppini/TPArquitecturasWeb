import { useEffect } from 'react'
import type { Session } from '../types'

/** Los temporizadores del navegador no admiten más de 2^31-1 ms. */
const MAX_DELAY = 2 ** 31 - 1

/** Cierra la sesión en el instante en que vence el token. */
export const useSessionExpiry = (session: Session | null, onExpire: () => void) => {
  useEffect(() => {
    if (!session) return undefined
    const delay = Math.min(Math.max(session.expiresAt - Date.now(), 0), MAX_DELAY)
    const timer = setTimeout(onExpire, delay)
    return () => clearTimeout(timer)
  }, [session, onExpire])
}
