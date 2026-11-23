import { Link } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'
import { formatEventDate } from '@/lib/format/date'
import type { EventDetail } from '../types/detail'
import { EventPoster } from './EventPoster'
import { EventStatusBadge } from './EventStatusBadge'

/** Cabecera del evento: póster, estado, título, fecha, sede y descripción. */
export const EventDetailHeader = ({ event }: { event: EventDetail }) => (
  <header className="flex flex-col gap-6">
    <Link to="/" className="inline-flex w-fit items-center gap-1 text-[14px] text-fg-secondary hover:text-fg">
      <Icon name="chevronLeft" className="size-4" />
      Volver a los eventos
    </Link>
    <div data-theme="dark" className="relative h-64 overflow-hidden rounded-xl bg-ink text-bone">
      <EventPoster eventId={event.id} />
    </div>
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <span className="text-overline text-brand-text">{event.category}</span>
        <EventStatusBadge status={event.status} />
      </div>
      <h1 className="text-h2 text-fg">{event.title}</h1>
      <p className="flex items-center gap-2 text-body-m text-fg-secondary">
        <Icon name="calendar" className="size-5 text-fg-muted" />
        {formatEventDate(event.startsAt)}
      </p>
      <p className="flex items-center gap-2 text-body-m text-fg-secondary">
        <Icon name="pin" className="size-5 text-fg-muted" />
        {[event.venue.name, event.venue.address, event.venue.city].filter(Boolean).join(' · ')}
      </p>
      {event.description && <p className="max-w-prose text-body-m text-fg-secondary">{event.description}</p>}
    </div>
  </header>
)
