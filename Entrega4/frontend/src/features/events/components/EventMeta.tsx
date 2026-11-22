import { Icon } from '@/components/ui/Icon'
import { formatEventDate } from '@/lib/format/date'

type EventMetaProps = {
  startsAt: string
  /** Se muestra solo si el backend la entrega. */
  venue?: string
}

export const EventMeta = ({ startsAt, venue }: EventMetaProps) => (
  <div className="flex flex-col gap-1.5 text-[14px] leading-5 text-fg-secondary">
    <span className="flex items-center gap-2">
      <Icon name="calendar" className="size-4 text-fg-muted" />
      {formatEventDate(startsAt)}
    </span>
    {venue && (
      <span className="flex items-center gap-2">
        <Icon name="pin" className="size-4 text-fg-muted" />
        {venue}
      </span>
    )}
  </div>
)
