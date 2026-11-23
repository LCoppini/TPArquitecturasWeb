import { Badge } from '@/components/ui/Badge'
import { useEventDetail } from '@/features/events/hooks/useEventDetail'
import { formatEventDate } from '@/lib/format/date'
import { formatPrice } from '@/lib/format/price'
import type { EntradaApi } from '../types'
import { statusOf } from '../utils/ticketStatus'
import { TicketQr } from './TicketQr'

/** Una entrada: evento, sector, asiento, estado y, si es válida, su código QR. */
export const TicketCard = ({ ticket }: { ticket: EntradaApi }) => {
  const { data: event } = useEventDetail(ticket.idEvento)
  const sector = event?.sectors.find(({ id }) => id === ticket.idSector)
  const status = statusOf(ticket.estado)
  const seat = ticket.fila !== null && ticket.asiento !== null ? `Fila ${ticket.fila} · Asiento ${ticket.asiento}` : 'Ubicación general'
  return (
    <article className="flex items-start justify-between gap-6 rounded-lg border border-line-subtle bg-surface p-5">
      <div className="flex flex-col gap-1.5">
        <Badge tone={status.tone}>{status.label}</Badge>
        <h3 className="text-title-card text-fg">{event?.title ?? `Evento ${ticket.idEvento}`}</h3>
        {event && (
          <p className="text-[14px] text-fg-secondary">
            {formatEventDate(event.startsAt)} · {event.venue.name}
          </p>
        )}
        <p className="text-[14px] text-fg-secondary">
          {sector?.name ?? `Sector ${ticket.idSector}`} · {seat}
        </p>
        <p className="text-caption text-fg-muted">Entrada Nº {ticket.id} · {formatPrice(ticket.precio)}</p>
      </div>
      {ticket.estado === 'VALIDA' && ticket.codigoQr && <TicketQr value={ticket.codigoQr} />}
    </article>
  )
}
