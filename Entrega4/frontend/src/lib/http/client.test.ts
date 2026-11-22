import { setAuthHandlers } from './authHandlers'
import { ApiError } from './ApiError'
import { http } from './client'
import { jsonResponse } from '@/test/render'

describe('http', () => {
  const fetchMock = vi.fn()
  const onUnauthorized = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    onUnauthorized.mockReset()
    vi.stubGlobal('fetch', fetchMock)
    setAuthHandlers({ getToken: () => 'abc', onUnauthorized })
  })
  afterEach(() => vi.unstubAllGlobals())

  it('agrega la base /api, la query y el token', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))
    await http('/eventos', { query: { tipo: 'RECITAL', pagina: 0, vacio: undefined } })
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/eventos?tipo=RECITAL&pagina=0')
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer abc')
  })

  it('cierra la sesión ante un 401 con token', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}, 401))
    await expect(http('/x')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('convierte una caída de red en ApiError con status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('failed'))
    await expect(http('/x')).rejects.toMatchObject({ status: 0 })
  })
})
