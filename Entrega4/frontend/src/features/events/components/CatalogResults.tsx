import { EmptyState } from '@/components/feedback/EmptyState'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Button } from '@/components/ui/Button'
import type { EventPage } from '../types/event'
import { EventCard } from './EventCard'
import { EventCardSkeleton } from './EventCardSkeleton'
import { EventGrid } from './EventGrid'

type CatalogResultsProps = {
  data?: EventPage
  isPending: boolean
  isError: boolean
  onRetry: () => void
  onClearFilters: () => void
}

const SKELETON_COUNT = 8

/** Elige qué mostrar según el estado de la consulta: carga, error, sin resultados o la grilla. */
export const CatalogResults = ({ data, isPending, isError, onRetry, onClearFilters }: CatalogResultsProps) => {
  if (isPending) {
    return (
      <EventGrid busy>
        {Array.from({ length: SKELETON_COUNT }, (_, index) => (
          <EventCardSkeleton key={index} />
        ))}
      </EventGrid>
    )
  }
  if (isError || !data) {
    return (
      <ErrorState
        title="No pudimos cargar los eventos"
        description="Hubo un problema al traer la cartelera. Intentá de nuevo en unos segundos."
        onRetry={onRetry}
      />
    )
  }
  if (data.items.length === 0) {
    return (
      <EmptyState
        title="No hay eventos para esta búsqueda"
        description="Probá con otra categoría o quitá los filtros para ver toda la cartelera."
        action={<Button variant="outline" onClick={onClearFilters}>Limpiar filtros</Button>}
      />
    )
  }
  return (
    <EventGrid>
      {data.items.map((event) => (
        <EventCard key={event.id} event={event} href={`/eventos/${event.id}`} />
      ))}
    </EventGrid>
  )
}
