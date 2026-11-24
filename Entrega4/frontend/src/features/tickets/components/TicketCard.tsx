import { Badge } from '@/components/ui/Badge'
import { Icon } from '@/components/ui/Icon'
import { useEventDetail } from '@/features/events/hooks/useEventDetail'
import { formatEventDate } from '@/lib/format/date'
import { formatPrice } from '@/lib/format/price'
import type { EntradaApi } from '../types'
import { statusOf } from '../utils/ticketStatus'
import { TicketQr } from './TicketQr'

/** Una entrada dentro de su compra: estado y precio arriba, evento, lugar y, si es válida, su QR a la derecha. */
export const TicketCard = ({ ticket }: { ticket: EntradaApi }) => {
  const { data: event } = useEventDetail(ticket.idEvento)
  const sector = event?.sectors.find(({ id }) => id === ticket.idSector)
  const status = statusOf(ticket.estado)
  const seat = ticket.fila !== null && ticket.asiento !== null ? `Fila ${ticket.fila} · Asiento ${ticket.asiento}` : 'Ubicación general'
  return (
    <article className="flex items-center gap-5 px-5 py-4">
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <div className="flex items-center gap-2.5">
          <Badge tone={status.tone}>{status.label}</Badge>
          <span className="text-caption text-fg-muted">Entrada Nº {ticket.id}</span>
          <span className="ml-auto text-[15px] leading-5 font-semibold text-fg">{formatPrice(ticket.precio)}</span>
        </div>
        <h3 className="text-title-card text-fg">{event?.title ?? `Evento ${ticket.idEvento}`}</h3>
        <div className="flex flex-col gap-1 text-[14px] leading-5 text-fg-secondary">
          {event && (
            <span className="flex items-center gap-2">
              <Icon name="calendar" className="size-4 text-fg-muted" />
              {formatEventDate(event.startsAt)} · {event.venue.name}
            </span>
          )}
          <span className="flex items-center gap-2">
            <Icon name="ticket" className="size-4 text-fg-muted" />
            {sector?.name ?? `Sector ${ticket.idSector}`} · {seat}
          </span>
        </div>
      </div>
      {ticket.estado === 'VALIDA' && ticket.codigoQr && <TicketQr value={ticket.codigoQr} />}
    </article>
  )
}
