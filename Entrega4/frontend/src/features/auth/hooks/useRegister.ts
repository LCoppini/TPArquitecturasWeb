import { useMutation } from '@tanstack/react-query'
import { authApi } from '../api/authApi'
import type { RegisterRequest } from '../types'
import { useAuth } from './useAuth'

/** Crea la cuenta y, si sale bien, ingresa con esas mismas credenciales. */
export const useRegister = () => {
  const { signIn } = useAuth()
  return useMutation({
    mutationFn: async (request: RegisterRequest) => {
      await authApi.register(request)
      const { token } = await authApi.login({ email: request.email, password: request.password })
      return signIn(token, request.email)
    },
  })
}
