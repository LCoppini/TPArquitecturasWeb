import { cn } from '@/lib/cn'
import { formatPrice } from '@/lib/format/price'

type EventPriceProps = {
  /** Menor precio del evento; null si no hay entradas para mostrar. */
  amount: number | null
  /** Evento sin venta (agotado, cancelado, etc.): se atenúa y no muestra precio. */
  muted?: boolean
  className?: string
}

export const EventPrice = ({ amount, muted = false, className }: EventPriceProps) => {
  const hasPrice = amount !== null && !muted
  return (
    <div className={cn('border-t border-line-subtle pt-3.5', className)}>
      <div className="text-caption text-fg-muted">{hasPrice ? 'Desde' : 'Sin entradas'}</div>
      <div className={cn('text-[26px] leading-[30px] font-medium font-serif', muted ? 'text-fg-muted' : 'text-fg')}>
        {hasPrice ? formatPrice(amount) : '—'}
      </div>
    </div>
  )
}
