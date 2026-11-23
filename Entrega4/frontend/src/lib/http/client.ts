import { getAuthHandlers } from './authHandlers'
import { networkError, toApiError } from './errorMessages'
import { buildQuery, type QueryParams } from './query'

const API_BASE = '/api'

export type HttpOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: QueryParams
  signal?: AbortSignal
  /** Cabeceras extra, por ejemplo `Idempotency-Key`. */
  headers?: Record<string, string>
}

const readJson = async (response: Response): Promise<unknown> => {
  if (response.status === 204) return undefined
  try {
    return await response.json()
  } catch {
    return undefined
  }
}

/** Única puerta de salida hacia el gateway: agrega el token, parsea JSON y normaliza los errores. */
export const http = async <T>(path: string, options: HttpOptions = {}): Promise<T> => {
  const { method = 'GET', body, query, signal, headers: extraHeaders } = options
  const { getToken, onUnauthorized } = getAuthHandlers()
  const token = getToken()

  const headers: Record<string, string> = { Accept: 'application/json', ...extraHeaders }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}${buildQuery(query)}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw networkError()
  }

  const data = await readJson(response)
  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized()
    throw toApiError(response.status, data)
  }
  return data as T
}
