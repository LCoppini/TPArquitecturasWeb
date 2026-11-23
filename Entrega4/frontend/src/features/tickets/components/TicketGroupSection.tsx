import { useState } from 'react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { describePurchaseError } from '@/features/purchase/utils/purchaseErrors'
import { useRefundPurchase } from '../hooks/useRefundPurchase'
import { canRefund, type TicketGroup } from '../utils/ticketStatus'
import { RefundStatusAlert } from './RefundStatusAlert'
import { TicketCard } from './TicketCard'

/** Entradas de una misma compra, con la opción de devolverla completa (con confirmación). */
export const TicketGroupSection = ({ group }: { group: TicketGroup }) => {
  const [confirming, setConfirming] = useState(false)
  const refund = useRefundPurchase()

  return (
    <section aria-label={group.purchaseId ? `Compra ${group.purchaseId}` : 'Entrada'} className="flex flex-col gap-3">
      {group.purchaseId !== null && (
        <div className="flex items-center justify-between">
          <h2 className="text-h3 text-fg">Compra #{group.purchaseId}</h2>
          {canRefund(group) && !confirming && (
            <Button variant="outline" size="sm" onClick={() => setConfirming(true)}>
              Devolver compra
            </Button>
          )}
          {confirming && (
            <div className="flex gap-2">
              <Button variant="text" size="sm" onClick={() => setConfirming(false)}>
                Cancelar
              </Button>
              <Button size="sm" loading={refund.isPending} onClick={() => refund.mutate(group.purchaseId as number, { onSettled: () => setConfirming(false) })}>
                Confirmar devolución
              </Button>
            </div>
          )}
        </div>
      )}
      <RefundStatusAlert group={group} />
      {refund.error && <Alert tone="error" title="No pudimos pedir la devolución">{describePurchaseError(refund.error)}</Alert>}
      {group.tickets.map((ticket) => (
        <TicketCard key={ticket.id} ticket={ticket} />
      ))}
    </section>
  )
}
