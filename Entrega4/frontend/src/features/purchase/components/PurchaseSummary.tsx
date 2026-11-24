import { formatPrice } from '@/lib/format/price'
import type { PurchaseSelection } from '../types'

/** Resumen de lo que se va a comprar: sector, cantidad y total. */
export const PurchaseSummary = ({ selection, total }: { selection: PurchaseSelection; total?: number | null }) => (
  <div className="flex items-center justify-between rounded-md border border-line-subtle bg-surface-alt px-4 py-3">
    <div>
      <div className="font-semibold text-fg">{selection.sectorName}</div>
      <div className="text-caption text-fg-muted">
        {selection.quantity} × {formatPrice(selection.unitPrice)}
      </div>
    </div>
    <div className="font-serif text-[22px] font-medium text-fg">
      {formatPrice(total ?? selection.unitPrice * selection.quantity)}
    </div>
  </div>
)
