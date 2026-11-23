import { useRef } from 'react'
import { Pagination } from '@/components/ui/Pagination'
import { useCatalogParams } from '../hooks/useCatalogParams'
import { useEvents } from '../hooks/useEvents'
import { CatalogResults } from './CatalogResults'
import { CategoryFilter } from './CategoryFilter'

const countLabel = (total: number) => `${total} ${total === 1 ? 'evento' : 'eventos'}`

/** Cartelera completa: filtro por categoría, resultados y paginación, sincronizados con la dirección. */
export const EventCatalog = () => {
  const { category, page, selectCategory, selectPage } = useCatalogParams()
  const events = useEvents({ category, page })
  const sectionRef = useRef<HTMLElement>(null)

  const changePage = (nextPage: number) => {
    selectPage(nextPage)
    sectionRef.current?.scrollIntoView({ block: 'start' })
  }

  return (
    <section ref={sectionRef} id="cartelera" aria-labelledby="catalog-title" className="flex scroll-mt-6 flex-col gap-8">
      <div className="flex flex-col gap-2">
        <span className="text-overline text-brand-text">Cartelera</span>
        <h2 id="catalog-title" className="text-h2">
          Próximos eventos
        </h2>
      </div>
      <div className="flex items-center justify-between gap-6">
        <CategoryFilter selected={category?.slug} onSelect={selectCategory} />
        {events.data && (
          <p role="status" className="text-body-s text-fg-secondary">
            {countLabel(events.data.totalItems)}
          </p>
        )}
      </div>
      <CatalogResults
        data={events.data}
        isPending={events.isPending}
        isError={events.isError}
        onRetry={() => void events.refetch()}
        onClearFilters={() => selectCategory(null)}
      />
      <Pagination page={events.data?.page ?? page} totalPages={events.data?.totalPages ?? 0} onPageChange={changePage} />
    </section>
  )
}
