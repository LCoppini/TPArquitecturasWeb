import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Skeleton } from '@/components/ui/Skeleton'
import { CityForm } from '../components/CityForm'
import { ProfileDetails } from '../components/ProfileDetails'
import { useProfile } from '../hooks/useProfile'

export const ProfilePage = () => {
  const { data, isPending, error, refetch } = useProfile()
  return (
    <Container className="flex max-w-2xl flex-col gap-8 pt-4">
      <h1 className="text-h2 text-fg">Mi perfil</h1>
      {isPending && <Skeleton aria-busy="true" className="h-64 rounded-xl" />}
      {error && <ErrorState title="No pudimos cargar tu perfil" description={error.message} onRetry={() => void refetch()} />}
      {data && <ProfileDetails user={data} />}
      {data && <CityForm currentCity={data.ciudad} />}
    </Container>
  )
}
