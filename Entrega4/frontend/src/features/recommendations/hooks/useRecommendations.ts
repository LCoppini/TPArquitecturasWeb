import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { recommendationsApi } from '../api/recommendationsApi'

/** Recomendaciones de la persona con sesión; sin sesión no se consultan. */
export const useRecommendations = () => {
  const { session } = useAuth()
  return useQuery({
    queryKey: ['recommendations', session?.idUsuario],
    queryFn: ({ signal }) => recommendationsApi.forClient(session!.idUsuario, signal),
    enabled: session !== null,
  })
}
