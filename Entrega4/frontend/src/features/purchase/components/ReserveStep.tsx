import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Countdown } from './Countdown'
import { PurchaseSummary } from './PurchaseSummary'
import type { IngresoApi, PurchaseSelection } from '../types'

type ReserveStepProps = {
  entry: IngresoApi
  selection: PurchaseSelection
  onReserve: () => void
  loading: boolean
  error: string | null
}

/** Es mi turno: confirmo lo elegido y reservo las entradas. El turno vence si no se completa la compra. */
export const ReserveStep = ({ entry, selection, onReserve, loading, error }: ReserveStepProps) => (
  <div className="flex flex-col gap-4">
    <Alert tone="success" title="¡Es tu turno!">
      <Countdown until={entry.venceEn} label="Tu turno vence en" />
    </Alert>
    <PurchaseSummary selection={selection} />
    {error && (
      <Alert tone="error" title="No pudimos reservar">
        {error}
      </Alert>
    )}
    <Button size="lg" loading={loading} onClick={onReserve}>
      Reservar entradas
    </Button>
  </div>
)
