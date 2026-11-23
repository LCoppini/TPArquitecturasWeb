import { Alert } from '@/components/ui/Alert'
import { formatPrice } from '@/lib/format/price'
import { refundStatusOf } from '../utils/refundStatus'
import type { TicketGroup } from '../utils/ticketStatus'

/** Cómo va la devolución de una compra: en proceso, rechazada o ya reintegrada. No muestra nada si no hubo devolución. */
export const RefundStatusAlert = ({ group }: { group: TicketGroup }) => {
  const status = refundStatusOf(group)
  if (!status) return null
  switch (status.kind) {
    case 'pending':
      return status.cancelled ? (
        <Alert tone="info" title="Reintegro en proceso">
          El evento se canceló y estamos reintegrando tu dinero.
        </Alert>
      ) : (
        <Alert tone="info" title="Devolución solicitada">
          La estamos procesando; te avisaremos cuando se acredite.
        </Alert>
      )
    case 'rejected':
      return (
        <Alert tone="warning" title="Devolución rechazada">
          No se realizó ningún reintegro de esta compra.
        </Alert>
      )
    case 'approved':
      return (
        <Alert tone="success" title={status.cancelled ? 'Reintegro realizado' : 'Devolución aprobada'}>
          Reintegramos {formatPrice(status.amount)}.
        </Alert>
      )
  }
}
