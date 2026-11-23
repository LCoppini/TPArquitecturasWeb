import { Link, useLocation } from 'react-router-dom'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { ApiError } from '@/lib/http'
import { useMyQueueEntry } from '../hooks/useMyQueueEntry'
import { useQueueActions } from '../hooks/useQueueActions'
import { useQueueInfo } from '../hooks/useQueueInfo'
import type { PurchaseSelection } from '../types'
import { CheckoutFlow } from './CheckoutFlow'
import { JoinQueue } from './JoinQueue'
import { QueueWaiting } from './QueueWaiting'

type PurchasePanelProps = {
  eventId: number
  /** Solo se puede comprar si el evento está publicado. */
  onSale: boolean
  selection: PurchaseSelection | null
}

const errorText = (error: unknown) => (error instanceof ApiError ? error.message : null)

/** Panel de compra: decide qué paso mostrar según la sesión, la fila del evento y mi lugar en ella. */
export const PurchasePanel = ({ eventId, onSale, selection }: PurchasePanelProps) => {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const info = useQueueInfo(eventId, isAuthenticated && onSale)
  const entry = useMyQueueEntry(eventId, isAuthenticated && onSale && Boolean(info.data))
  const { join, leave } = useQueueActions(eventId)

  if (!onSale) return null
  if (!isAuthenticated) {
    return (
      <Button asChild size="lg" className="w-full">
        <Link to="/ingresar" state={{ from: location.pathname }}>
          Ingresá para comprar
        </Link>
      </Button>
    )
  }
  if (info.isPending || (info.data && entry.isPending)) return <Spinner />
  if (info.isError) return <Alert tone="error" title="No pudimos consultar la venta">Intentá de nuevo en unos instantes.</Alert>
  if (!info.data) return <Alert tone="info" title="La venta todavía no está habilitada">El organizador aún no abrió la fila de este evento.</Alert>
  if (!selection) return <Alert tone="info" title="Elegí un sector">Seleccioná un sector y la cantidad de entradas para continuar.</Alert>

  const current = entry.data ?? null
  if (current?.estado === 'EN_ESPERA') {
    return <QueueWaiting entry={current} onLeave={() => leave.mutate()} leaving={leave.isPending} />
  }
  if (current?.estado === 'HABILITADO') return <CheckoutFlow eventId={eventId} entry={current} selection={selection} />
  return <JoinQueue previous={current} onJoin={() => join.mutate()} loading={join.isPending} error={errorText(join.error)} />
}
