import { EventCard } from '@/features/events/components/EventCard'
import { useEventDetail } from '@/features/events/hooks/useEventDetail'
import { Skeleton } from '@/components/ui/Skeleton'
import type { EventSummary } from '@/features/events/types/event'
import type { RecomendacionApi } from '../api/recommendationsApi'
import { reasonLabel } from '../utils/reasonLabel'

/** Tarjeta de un evento recomendado: resuelve el evento y muestra el motivo de la recomendación. */
export const RecommendationCard = ({ recommendation }: { recommendation: RecomendacionApi }) => {
  const { data } = useEventDetail(recommendation.idEvento)
  if (!data) return <Skeleton className="h-96 rounded-xl" />
  const summary: EventSummary = {
    id: data.id,
    title: data.title,
    category: data.category,
    startsAt: data.startsAt,
    fromPrice: data.sectors.length ? Math.min(...data.sectors.map(({ price }) => price)) : null,
    status: data.status,
    venue: data.venue.name,
  }
  const reason = reasonLabel(recommendation.motivo)
  return (
    <div className="flex flex-col gap-2">
      <EventCard event={summary} href={`/eventos/${data.id}`} />
      {reason && <p className="px-1 text-caption text-fg-muted">{reason}</p>}
    </div>
  )
}
