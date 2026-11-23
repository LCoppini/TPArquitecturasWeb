import { useState } from 'react'
import { PurchasePanel } from '@/features/purchase/components/PurchasePanel'
import type { PurchaseSelection } from '@/features/purchase/types'
import type { EventDetail } from '../types/detail'
import { QuantityStepper } from './QuantityStepper'
import { SectorOption } from './SectorOption'

/** Máximo de entradas por compra que admite el backend. */
const MAX_PER_PURCHASE = 6

/** Tarjeta lateral: elegir sector y cantidad, y avanzar por la fila, la reserva y el pago. */
export const PurchaseCard = ({ event }: { event: EventDetail }) => {
  const [sectorId, setSectorId] = useState<number | null>(null)
  const [available, setAvailable] = useState(0)
  const [quantity, setQuantity] = useState(1)

  const sector = event.sectors.find(({ id }) => id === sectorId)
  const max = Math.max(Math.min(MAX_PER_PURCHASE, available), 1)
  const selection: PurchaseSelection | null = sector
    ? { sectorId: sector.id, sectorName: sector.name, unitPrice: sector.price, quantity: Math.min(quantity, max) }
    : null
  const onSale = event.status === 'available'

  return (
    <aside aria-label="Comprar entradas" className="flex flex-col gap-5 rounded-xl border border-line-subtle bg-surface p-6 shadow-sm">
      <h2 className="text-h3 text-fg">Entradas</h2>
      {event.sectors.length === 0 && <p className="text-body-m text-fg-secondary">Este evento todavía no tiene entradas a la venta.</p>}
      <div role="radiogroup" aria-label="Sector" className="flex flex-col gap-2">
        {event.sectors.map((option) => (
          <SectorOption
            key={option.id}
            eventId={event.id}
            sector={option}
            selected={option.id === sectorId}
            onSelect={(count) => {
              setSectorId(option.id)
              setAvailable(count)
            }}
          />
        ))}
      </div>
      {sector && onSale && <QuantityStepper value={selection?.quantity ?? 1} max={max} onChange={setQuantity} />}
      <PurchasePanel eventId={event.id} onSale={onSale} selection={selection} />
    </aside>
  )
}
