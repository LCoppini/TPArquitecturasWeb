import { ApiError } from '@/lib/http'

export type AuthErrorMessage = { title: string; message: string }

/** Mensaje para mostrar cuando falla el ingreso. */
export const describeLoginError = (error: unknown): AuthErrorMessage => {
  if (error instanceof ApiError) {
    if (error.isUnauthorized) {
      return { title: 'Correo o contraseña incorrectos', message: 'Revisá tus datos e intentá de nuevo.' }
    }
    return { title: 'No pudimos iniciar tu sesión', message: error.message }
  }
  return { title: 'No pudimos iniciar tu sesión', message: 'Intentá de nuevo en unos minutos.' }
}

/** Mensaje para mostrar cuando falla el registro. */
export const describeRegisterError = (error: unknown): AuthErrorMessage => {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      return {
        title: 'Ya hay una cuenta con esos datos',
        message: 'El correo o el DNI ya están registrados. Ingresá a tu cuenta o usá otros datos.',
      }
    }
    return { title: 'No pudimos crear tu cuenta', message: error.message }
  }
  return { title: 'No pudimos crear tu cuenta', message: 'Intentá de nuevo en unos minutos.' }
}
