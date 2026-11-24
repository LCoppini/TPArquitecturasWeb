export type FieldErrors = Record<string, string>

/** Error uniforme de toda llamada al backend. `status` 0 indica que no hubo respuesta (red). */
export class ApiError extends Error {
  readonly status: number
  readonly details: FieldErrors
  /** Cuerpo de la respuesta de error, por si el endpoint devuelve un código propio (por ejemplo `codigo`). */
  readonly data: unknown

  constructor(status: number, message: string, details: FieldErrors = {}, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
    this.data = data
  }

  get isNetwork() {
    return this.status === 0
  }

  get isUnauthorized() {
    return this.status === 401
  }
}
