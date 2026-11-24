import { useMutation } from '@tanstack/react-query'
import { authApi } from '../api/authApi'
import type { LoginCredentials } from '../types'
import { useAuth } from './useAuth'

/** Ingresa con correo y contraseña y deja la sesión iniciada. */
export const useLogin = () => {
  const { signIn } = useAuth()
  return useMutation({
    mutationFn: async (credentials: LoginCredentials) => {
      const { token } = await authApi.login(credentials)
      return signIn(token, credentials.email)
    },
  })
}
