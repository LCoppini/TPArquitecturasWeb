import { jsonResponse } from './render'

type Reply = unknown | ((request: { body: unknown; headers: Record<string, string> }) => Response)

/**
 * Simula el gateway: cada clave es "MÉTODO /ruta" (sin query) y el valor, el cuerpo de la respuesta
 * o una función que devuelve la `Response`. Lo que no está registrado responde 404.
 * Devuelve el mock de fetch para inspeccionar las llamadas.
 */
export const mockApi = (routes: Record<string, Reply>) => {
  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    const path = url.split('?')[0].replace(/^\/api/, '')
    const reply = routes[`${init.method ?? 'GET'} ${path}`]
    if (reply === undefined) return jsonResponse({ message: 'No encontrado' }, 404)
    if (typeof reply === 'function') {
      const body = typeof init.body === 'string' ? JSON.parse(init.body) : undefined
      return (reply as (request: unknown) => Response)({ body, headers: (init.headers ?? {}) as Record<string, string> })
    }
    return jsonResponse(reply)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export const callsTo = (fetchMock: ReturnType<typeof mockApi>, method: string, path: string) =>
  fetchMock.mock.calls.filter(([url, init]) => url.split('?')[0] === `/api${path}` && (init?.method ?? 'GET') === method)

export const signInAs = (token: string, email = 'vale@correo.com') =>
  sessionStorage.setItem('ticketya.session', JSON.stringify({ token, email }))
