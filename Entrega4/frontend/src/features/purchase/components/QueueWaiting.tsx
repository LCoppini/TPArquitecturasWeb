import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { formatDuration } from '@/lib/format/duration'
import type { IngresoApi } from '../types'

type QueueWaitingProps = {
  entry: IngresoApi
  onLeave: () => void
  leaving: boolean
}

/** Pantalla de espera: lugar en la fila, personas delante y tiempo estimado, con la opción de salir. */
export const QueueWaiting = ({ entry, onLeave, leaving }: QueueWaitingProps) => {
  const ahead = Math.max((entry.posicion ?? 1) - 1, 0)
  return (
    <div className="flex flex-col gap-4">
      <Alert tone="info" title="Estás en la fila virtual">
        Mantené esta página abierta: cuando sea tu turno vas a poder elegir y pagar tus entradas.
      </Alert>
      <dl className="grid grid-cols-3 gap-3 text-center">
        <Stat label="Tu lugar" value={entry.posicion === null ? '—' : `#${entry.posicion}`} />
        <Stat label="Personas delante" value={String(ahead)} />
        <Stat
          label="Espera estimada"
          value={entry.esperaEstimadaSegundos === null ? 'Calculando' : `hasta ${formatDuration(entry.esperaEstimadaSegundos)}`}
        />
      </dl>
      <Button variant="outline" loading={leaving} onClick={onLeave}>
        Salir de la fila
      </Button>
    </div>
  )
}

const Stat = ({ label, value }: { label: string; value: string }) => (
  <div className="rounded-md border border-line-subtle bg-surface-alt px-2 py-3">
    <dt className="text-caption text-fg-muted">{label}</dt>
    <dd className="text-[18px] leading-6 font-semibold text-fg">{value}</dd>
  </div>
)
