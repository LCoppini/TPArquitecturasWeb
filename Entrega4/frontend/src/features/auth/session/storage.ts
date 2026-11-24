import type { Session } from '../types'
import { buildSession } from './buildSession'

const KEY = 'ticketya.session'

type StoredSession = Pick<Session, 'token' | 'email'>

export const storeSession = ({ token, email }: StoredSession) => {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ token, email }))
  } catch {
    // Sin almacenamiento disponible: la sesión dura solo mientras la página siga abierta.
  }
}

export const clearStoredSession = () => {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // Nada que limpiar.
  }
}

/** Recupera la sesión guardada. Si el token venció o está dañado, la descarta. */
export const restoreSession = (): Session | null => {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const { token, email } = JSON.parse(raw) as Partial<StoredSession>
    const session = typeof token === 'string' && typeof email === 'string' ? buildSession(token, email) : null
    if (!session) clearStoredSession()
    return session
  } catch {
    clearStoredSession()
    return null
  }
}
