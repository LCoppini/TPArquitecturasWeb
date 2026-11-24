import { Link, useParams } from 'react-router-dom'
import { Container } from '@/components/layout/Container'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Button } from '@/components/ui/Button'
import { ApiError } from '@/lib/http'
import { EventDetailHeader } from '../components/EventDetailHeader'
import { EventDetailSkeleton } from '../components/EventDetailSkeleton'
import { EventLineup } from '../components/EventLineup'
import { PurchaseCard } from '../components/PurchaseCard'
import { useEventDetail } from '../hooks/useEventDetail'

const backHome = (
  <Button asChild variant="outline">
    <Link to="/">Ver todos los eventos</Link>
  </Button>
)

export const EventDetailPage = () => {
  const { id } = useParams()
  const { data, isPending, error, refetch } = useEventDetail(Number(id))

  if (isPending) return <EventDetailSkeleton />
  if (error) {
    const notFound = error instanceof ApiError && error.status === 404
    return (
      <Container>
        <ErrorState
          title={notFound ? 'No encontramos este evento' : 'No pudimos cargar el evento'}
          description={notFound ? 'Puede que haya sido eliminado o que el enlace sea incorrecto.' : error.message}
          onRetry={notFound ? undefined : () => void refetch()}
          action={backHome}
        />
      </Container>
    )
  }
  return (
    <Container className="grid grid-cols-[1fr_420px] items-start gap-12 pt-4">
      <div className="flex flex-col gap-10">
        <EventDetailHeader event={data} />
        <EventLineup lineup={data.lineup} />
      </div>
      <div className="sticky top-6">
        <PurchaseCard event={data} />
      </div>
    </Container>
  )
}
