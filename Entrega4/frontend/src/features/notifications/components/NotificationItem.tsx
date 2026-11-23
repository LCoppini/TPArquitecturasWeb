import { Badge } from '@/components/ui/Badge'
import { formatEventDate } from '@/lib/format/date'
import type { NotificacionApi } from '../types'
import { badgeOf, typeLabel } from '../utils/notificationStatus'

/** Un aviso: qué fue, su asunto, cuándo se generó y si llegó. */
export const NotificationItem = ({ notification }: { notification: NotificacionApi }) => {
  const badge = badgeOf(notification.estado)
  return (
    <li className="flex items-start justify-between gap-4 rounded-lg border border-line-subtle bg-surface p-5">
      <div className="flex min-w-0 flex-col gap-1">
        <h3 className="text-title-card text-fg">{typeLabel(notification.tipo)}</h3>
        {notification.asunto && <p className="truncate text-[14px] text-fg-secondary">{notification.asunto}</p>}
        <p className="text-caption text-fg-muted">{formatEventDate(notification.fechaEnvio ?? notification.fechaCreacion)}</p>
      </div>
      <Badge tone={badge.tone}>{badge.label}</Badge>
    </li>
  )
}
