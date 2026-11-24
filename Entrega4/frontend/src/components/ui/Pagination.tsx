import { cn } from '@/lib/cn'
import { getPageItems } from '@/lib/pagination'
import { Icon } from './Icon'

type PaginationProps = {
  /** Página actual, empezando en 1. */
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

const BUTTON = 'grid h-11 min-w-11 cursor-pointer place-items-center rounded-full text-[15px] leading-5 font-semibold text-fg tabular-nums hover:bg-surface-alt'
const EDGE = 'border-[1.5px] border-line bg-surface'
const DISABLED = 'cursor-not-allowed bg-transparent text-fg-disabled hover:bg-transparent'

export const Pagination = ({ page, totalPages, onPageChange }: PaginationProps) => {
  if (totalPages <= 1) return null
  const isFirst = page <= 1
  const isLast = page >= totalPages

  return (
    <nav aria-label="Paginación" className="flex items-center justify-center gap-1.5">
      <button type="button" aria-label="Página anterior" disabled={isFirst} onClick={() => onPageChange(page - 1)} className={cn(BUTTON, EDGE, isFirst && DISABLED)}>
        <Icon name="chevronLeft" />
      </button>
      {getPageItems(page, totalPages).map((item, index) =>
        item === 'gap' ? (
          <span key={`gap-${index}`} aria-hidden="true" className="w-7 text-center text-fg-muted">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            aria-label={`Página ${item}`}
            aria-current={item === page ? 'page' : undefined}
            onClick={() => onPageChange(item)}
            className={cn(BUTTON, item === page && 'bg-brand text-on-brand hover:bg-brand-hover')}
          >
            {item}
          </button>
        ),
      )}
      <button type="button" aria-label="Página siguiente" disabled={isLast} onClick={() => onPageChange(page + 1)} className={cn(BUTTON, EDGE, isLast && DISABLED)}>
        <Icon name="chevronRight" />
      </button>
    </nav>
  )
}
