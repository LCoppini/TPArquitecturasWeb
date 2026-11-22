import { Badge } from '@/components/ui/Badge'
import type { EventSaleStatus } from '../types/event'

/** Insignia del estado de venta del evento. */
export const EventStatusBadge = ({ status }: { status: EventSaleStatus }) => {
  switch (status) {
    case 'available':
      return (
        <Badge tone="success" dot>
          Disponible
        </Badge>
      )
    case 'soldout':
      return <Badge>Agotado</Badge>
    case 'cancelled':
      return (
        <Badge tone="error" icon="alertCircle">
          Cancelado
        </Badge>
      )
    case 'suspended':
      return (
        <Badge tone="warning" icon="clock">
          Suspendido
        </Badge>
      )
    case 'finished':
      return <Badge>Finalizado</Badge>
  }
}
