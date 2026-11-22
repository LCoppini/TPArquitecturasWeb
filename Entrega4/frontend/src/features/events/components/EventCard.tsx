import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'
import type { EventSummary } from '../types/event'
import { EventDateBadge } from './EventDateBadge'
import { EventMeta } from './EventMeta'
import { EventPoster } from './EventPoster'
import { EventPrice } from './EventPrice'
import { EventStatusBadge } from './EventStatusBadge'

type EventCardProps = {
  event: EventSummary
  /** Si se indica, el título enlaza al detalle del evento. */
  href?: string
}

export const EventCard = ({ event, href }: EventCardProps) => {
  const onSale = event.status === 'available'
  return (
    <article
      className={cn(
        'relative flex flex-col overflow-hidden rounded-xl border border-line-subtle bg-surface text-fg shadow-sm',
        'motion-base transition-[box-shadow,transform] hover:-translate-y-0.75 hover:shadow-md',
      )}
    >
      <div data-theme="dark" className={cn('relative h-47.5 overflow-hidden bg-ink text-bone', !onSale && 'brightness-75 grayscale')}>
        <EventPoster eventId={event.id} />
        <EventDateBadge startsAt={event.startsAt} />
        <div className="absolute bottom-3.5 left-3.5">
          <EventStatusBadge status={event.status} />
        </div>
      </div>
      <div className="flex flex-1 flex-col gap-2.5 px-5 pt-4.5 pb-5">
        <span className="text-overline text-brand-text">{event.category}</span>
        <h3 className={cn('min-h-15 text-title-card', !onSale && 'text-fg-muted')}>
          {href ? (
            <Link to={href} className="text-inherit no-underline after:absolute after:inset-0 after:content-['']">
              {event.title}
            </Link>
          ) : (
            event.title
          )}
        </h3>
        <EventMeta startsAt={event.startsAt} venue={event.venue} />
        <EventPrice className="mt-auto" amount={event.fromPrice} muted={!onSale} />
      </div>
    </article>
  )
}
