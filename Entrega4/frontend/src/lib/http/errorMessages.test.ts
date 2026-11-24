import { networkError, toApiError } from './errorMessages'

describe('toApiError', () => {
  it('usa el mensaje y los detalles del backend', () => {
    const error = toApiError(400, { message: 'Datos inválidos', details: { email: 'formato inválido' } })
    expect(error.message).toBe('Datos inválidos')
    expect(error.details).toEqual({ email: 'formato inválido' })
  })

  it('usa un mensaje por defecto si el cuerpo no trae uno', () => {
    expect(toApiError(401, undefined).message).toMatch(/sesión/i)
    expect(toApiError(503, null).message).toMatch(/nuestro lado/i)
  })

  it('ignora detalles con forma inesperada', () => {
    expect(toApiError(400, { details: { campo: 3 } }).details).toEqual({})
  })

  it('marca los errores de red con status 0', () => {
    expect(networkError().isNetwork).toBe(true)
    expect(toApiError(401, {}).isUnauthorized).toBe(true)
  })
})
