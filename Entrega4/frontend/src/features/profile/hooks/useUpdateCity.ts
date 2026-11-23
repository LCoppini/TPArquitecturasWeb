import { useMutation, useQueryClient } from '@tanstack/react-query'
import { profileApi } from '../api/profileApi'

/** Guarda la ciudad y deja el perfil en caché con el dato nuevo, sin volver a pedirlo. */
export const useUpdateCity = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ciudad: string) => profileApi.updateCity(ciudad),
    onSuccess: (profile) => queryClient.setQueryData(['profile'], profile),
  })
}
