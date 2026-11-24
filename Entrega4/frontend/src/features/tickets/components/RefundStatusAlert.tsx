import { Icon } from '@/components/ui/Icon'
import type { IconName } from '@/components/ui/iconPaths'
import { cn } from '@/lib/cn'
import { formatPrice } from '@/lib/format/price'
import { refundStatusOf, type RefundStatus } from '../utils/refundStatus'
import type { TicketGroup } from '../utils/ticketStatus'

type Note = { tone: 'info' | 'warning' | 'success'; icon: IconName; title: string; text: string }

const TONES = {
  info: 'border-info-border bg-info-bg text-info-fg',
  warning: 'border-warning-border bg-warning-bg text-warning-fg',
  success: 'border-success-border bg-success-bg text-success-fg',
}

const noteOf = (status: RefundStatus): Note => {
  switch (status.kind) {
    case 'pending':
      return status.cancelled
        ? { tone: 'info', icon: 'info', title: 'Reintegro en proceso', text: 'El evento se canceló y estamos reintegrando tu dinero.' }
        : { tone: 'info', icon: 'info', title: 'Devolución solicitada', text: 'La estamos procesando; te avisaremos cuando se acredite.' }
    case 'rejected':
      return { tone: 'warning', icon: 'alertCircle', title: 'Devolución rechazada', text: 'No se realizó ningún reintegro de esta compra.' }
    case 'approved':
      return {
        tone: 'success',
        icon: 'checkCircle',
        title: status.cancelled ? 'Reintegro realizado' : 'Devolución aprobada',
        text: `Reintegramos ${formatPrice(status.amount)}.`,
      }
  }
}

/** Franja delgada con el estado de la devolución de la compra. No muestra nada si no hubo devolución. */
export const RefundStatusAlert = ({ group }: { group: TicketGroup }) => {
  const status = refundStatusOf(group)
  if (!status) return null
  const { tone, icon, title, text } = noteOf(status)
  return (
    <div role="status" className={cn('flex items-start gap-2.5 border-y px-5 py-2.5 text-[14px] leading-5', TONES[tone])}>
      <Icon name={icon} className="mt-0.5 size-4" />
      <p className="text-fg">
        <strong className="mr-1.5 font-bold text-current">{title}</strong>
        <span>{text}</span>
      </p>
    </div>
  )
}
