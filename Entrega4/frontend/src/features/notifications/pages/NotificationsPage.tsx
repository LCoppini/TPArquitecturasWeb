import { useState } from 'react'
import { EmptyState } from '@/components/feedback/EmptyState'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Skeleton } from '@/components/ui/Skeleton'
import { NotificationFilters } from '../components/NotificationFilters'
import { NotificationItem } from '../components/NotificationItem'
import { useMyNotifications } from '../hooks/useMyNotifications'
import { countNotifications, filterNotifications, type NotificationFilter } from '../utils/notificationStatus'

export const NotificationsPage = () => {
  const { data, isPending, error, refetch } = useMyNotifications()
  const [filter, setFilter] = useState<NotificationFilter>('all')
  const visible = data ? filterNotifications(data, filter) : []

  return (
    <Container className="flex max-w-3xl flex-col gap-6 pt-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-h2 text-fg">Notificaciones</h1>
        <p className="text-body-m text-fg-secondary">El historial de los avisos que te mandamos por compras, devoluciones y cambios de eventos.</p>
      </div>
      {isPending && (
        <div aria-busy="true" className="flex flex-col gap-3">
          <Skeleton className="h-24 rounded-lg" />
          <Skeleton className="h-24 rounded-lg" />
          <Skeleton className="h-24 rounded-lg" />
        </div>
      )}
      {error && <ErrorState title="No pudimos cargar tus notificaciones" description={error.message} onRetry={() => void refetch()} />}
      {data && data.length === 0 && (
        <EmptyState icon="mail" title="Todavía no tenés notificaciones" description="Cuando compres o haya cambios en tus eventos, los avisos aparecen acá." />
      )}
      {data && data.length > 0 && <NotificationFilters active={filter} counts={countNotifications(data)} onChange={setFilter} />}
      {data && data.length > 0 && visible.length === 0 && (
        <EmptyState icon="mail" title="No hay notificaciones en esta categoría" description="Probá con otro filtro para ver el resto." />
      )}
      {visible.length > 0 && (
        <ul aria-label="Historial de notificaciones" className="flex flex-col gap-3">
          {visible.map((notification) => (
            <NotificationItem key={notification.id} notification={notification} />
          ))}
        </ul>
      )}
    </Container>
  )
}
