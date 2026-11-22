export type FieldErrors = Record<string, string>

/** Error uniforme de toda llamada al backend. `status` 0 indica que no hubo respuesta (red). */
export class ApiError extends Error {
  readonly status: number
  readonly details: FieldErrors

  constructor(status: number, message: string, details: FieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }

  get isNetwork() {
    return this.status === 0
  }

  get isUnauthorized() {
    return this.status === 401
  }
}
