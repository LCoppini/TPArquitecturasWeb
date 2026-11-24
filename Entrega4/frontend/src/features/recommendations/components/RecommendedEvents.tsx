import { EventGrid } from '@/features/events/components/EventGrid'
import { useRecommendations } from '../hooks/useRecommendations'
import { RecommendationCard } from './RecommendationCard'

const MAX_ITEMS = 4

/** "Para vos": solo aparece con sesión y si hay recomendaciones; si fallan, no estorba al catálogo. */
export const RecommendedEvents = () => {
  const { data } = useRecommendations()
  if (!data || data.length === 0) return null
  return (
    <section aria-labelledby="recommended-title" className="flex flex-col gap-6">
      <h2 id="recommended-title" className="text-h3 text-fg">
        Recomendados para vos
      </h2>
      <EventGrid>
        {data.slice(0, MAX_ITEMS).map((recommendation) => (
          <RecommendationCard key={recommendation.idEvento} recommendation={recommendation} />
        ))}
      </EventGrid>
    </section>
  )
}
