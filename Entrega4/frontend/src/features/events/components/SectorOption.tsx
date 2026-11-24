import { cn } from '@/lib/cn'
import { formatPrice } from '@/lib/format/price'
import { useSectorAvailability } from '../hooks/useSectorAvailability'
import type { SectorInfo } from '../types/detail'

type SectorOptionProps = {
  eventId: number
  sector: SectorInfo
  selected: boolean
  onSelect: (available: number) => void
}

/** Un sector elegible con su precio y cuántas entradas quedan. Sin entradas no se puede elegir. */
export const SectorOption = ({ eventId, sector, selected, onSelect }: SectorOptionProps) => {
  const { data, isPending } = useSectorAvailability(eventId, sector.id)
  const soldOut = data !== undefined && data.available === 0
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={soldOut || isPending}
      onClick={() => data && onSelect(data.available)}
      className={cn(
        'flex w-full cursor-pointer items-center justify-between gap-3 rounded-md border px-4 py-3 text-left',
        selected ? 'border-brand bg-brand/5' : 'border-line bg-surface hover:border-line-strong',
        'disabled:cursor-not-allowed disabled:opacity-60',
      )}
    >
      <span>
        <span className="block font-semibold text-fg">{sector.name}</span>
        <span className="block text-caption text-fg-muted">
          {isPending ? 'Consultando…' : soldOut ? 'Agotado' : `${data?.available} disponibles`}
        </span>
      </span>
      <span className="font-serif text-[20px] font-medium text-fg">{formatPrice(sector.price)}</span>
    </button>
  )
}
