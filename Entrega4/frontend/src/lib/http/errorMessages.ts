import { ApiError, type FieldErrors } from './ApiError'

const FALLBACK_MESSAGES: Record<number, string> = {
  400: 'Revisá los datos e intentá de nuevo.',
  401: 'Tu sesión no es válida. Ingresá de nuevo.',
  403: 'No tenés permiso para hacer esto.',
  404: 'No encontramos lo que buscabas.',
  409: 'La operación entra en conflicto con un dato existente.',
}

export const NETWORK_MESSAGE = 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
const SERVER_MESSAGE = 'Algo salió mal de nuestro lado. Intentá de nuevo en unos minutos.'

type ErrorBody = { message?: unknown; details?: unknown }

const isFieldErrors = (value: unknown): value is FieldErrors =>
  typeof value === 'object' && value !== null && Object.values(value).every((v) => typeof v === 'string')

/** Traduce una respuesta de error (con o sin cuerpo `ErrorResponse`) a un `ApiError`. */
export const toApiError = (status: number, body: unknown): ApiError => {
  const { message, details } = (typeof body === 'object' && body !== null ? body : {}) as ErrorBody
  const fallback = FALLBACK_MESSAGES[status] ?? (status >= 500 ? SERVER_MESSAGE : 'No pudimos completar la operación.')
  return new ApiError(
    status,
    typeof message === 'string' && message.length > 0 ? message : fallback,
    isFieldErrors(details) ? details : {},
    body,
  )
}

export const networkError = () => new ApiError(0, NETWORK_MESSAGE)
