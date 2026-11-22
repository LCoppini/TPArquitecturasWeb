import { makeToken, secondsFromNow } from '@/test/jwt'
import { buildSession } from './buildSession'
import { decodeClaims } from './claims'
import { restoreSession, storeSession } from './storage'

describe('claims y sesión', () => {
  it('lee los claims de un token válido', () => {
    const token = makeToken({ idUsuario: 7, roles: ['CLIENTE'], exp: secondsFromNow(60) })
    expect(decodeClaims(token)).toMatchObject({ idUsuario: 7, roles: ['CLIENTE'] })
  })

  it('devuelve null con tokens mal formados', () => {
    expect(decodeClaims('basura')).toBeNull()
    expect(decodeClaims('a.%%%.c')).toBeNull()
    expect(decodeClaims(`a.${btoa('{"idUsuario":"x"}')}.c`)).toBeNull()
  })

  it('rechaza un token vencido', () => {
    expect(buildSession(makeToken({ exp: secondsFromNow(-5) }), 'a@b.com')).toBeNull()
  })

  it('arma la sesión con el vencimiento en milisegundos', () => {
    const exp = secondsFromNow(600)
    expect(buildSession(makeToken({ exp }), 'a@b.com')).toMatchObject({ email: 'a@b.com', expiresAt: exp * 1000 })
  })

  it('restaura la sesión guardada y descarta la inválida', () => {
    storeSession({ token: makeToken({ exp: secondsFromNow(600) }), email: 'a@b.com' })
    expect(restoreSession()?.email).toBe('a@b.com')

    sessionStorage.setItem('ticketya.session', '{no es json')
    expect(restoreSession()).toBeNull()
    expect(sessionStorage.getItem('ticketya.session')).toBeNull()
  })
})
