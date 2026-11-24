import { Alert } from '@/components/ui/Alert'
import { formatPrice } from '@/lib/format/price'
import { describePurchaseError } from '@/features/purchase/utils/purchaseErrors'
import { useRefundPurchase } from '../hooks/useRefundPurchase'
import { canRefund, type TicketGroup } from '../utils/ticketStatus'
import { RefundAction } from './RefundAction'
import { RefundStatusAlert } from './RefundStatusAlert'
import { TicketCard } from './TicketCard'

const countLabel = (count: number) => (count === 1 ? '1 entrada' : `${count} entradas`)

/** Una compra como tarjeta: cabecera con resumen y devolución, estado del reintegro y sus entradas en filas. */
export const TicketGroupSection = ({ group }: { group: TicketGroup }) => {
  const refund = useRefundPurchase()
  const total = group.tickets.reduce((sum, { precio }) => sum + precio, 0)

  return (
    <section
      aria-label={group.purchaseId ? `Compra ${group.purchaseId}` : 'Entrada'}
      className="overflow-hidden rounded-xl border border-line-subtle bg-surface shadow-sm"
    >
      {group.purchaseId !== null && (
        <header className="flex items-center justify-between gap-4 border-b border-line-subtle px-5 py-3.5">
          <div>
            <h2 className="text-title-card text-fg">Compra #{group.purchaseId}</h2>
            <p className="text-caption text-fg-muted">
              {countLabel(group.tickets.length)} · {formatPrice(total)}
            </p>
          </div>
          {canRefund(group) && <RefundAction purchaseId={group.purchaseId} refund={refund} />}
        </header>
      )}
      <RefundStatusAlert group={group} />
      {refund.error && (
        <div className="px-5 pt-4">
          <Alert tone="error" title="No pudimos pedir la devolución">
            {describePurchaseError(refund.error)}
          </Alert>
        </div>
      )}
      <div className="divide-y divide-dashed divide-line">
        {group.tickets.map((ticket) => (
          <TicketCard key={ticket.id} ticket={ticket} />
        ))}
      </div>
    </section>
  )
}
