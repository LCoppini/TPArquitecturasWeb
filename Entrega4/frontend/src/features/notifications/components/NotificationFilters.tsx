import { Chip } from '@/components/ui/Chip'
import { NOTIFICATION_FILTERS, type NotificationFilter } from '../utils/notificationStatus'

type NotificationFiltersProps = {
  active: NotificationFilter
  counts: Record<NotificationFilter, number>
  onChange: (filter: NotificationFilter) => void
}

export const NotificationFilters = ({ active, counts, onChange }: NotificationFiltersProps) => (
  <div role="group" aria-label="Filtrar notificaciones" className="flex flex-wrap gap-2">
    {NOTIFICATION_FILTERS.map(({ id, label }) => (
      <Chip key={id} pressed={active === id} count={counts[id]} onClick={() => onChange(id)}>
        {label}
      </Chip>
    ))}
  </div>
)
