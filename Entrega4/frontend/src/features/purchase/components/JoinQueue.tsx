import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import type { IngresoApi } from '../types'

const MESSAGES: Record<string, { tone: 'info' | 'warning'; title: string; text: string }> = {
  SALIO: { tone: 'info', title: 'Saliste de la fila', text: 'Podés volver a entrar cuando quieras.' },
  VENCIDO: { tone: 'warning', title: 'Tu turno venció', text: 'No completaste la compra a tiempo. Podés volver a ponerte en la fila.' },
  COMPRO: { tone: 'info', title: 'Ya compraste en este evento', text: 'Si querés más entradas, volvé a ponerte en la fila.' },
  CANCELADO: { tone: 'warning', title: 'Tu turno se canceló', text: 'Podés volver a ponerte en la fila.' },
}

type JoinQueueProps = {
  previous: IngresoApi | null
  onJoin: () => void
  loading: boolean
  error: string | null
}

/** Punto de entrada a la compra: se ingresa a la fila virtual antes de elegir pago. */
export const JoinQueue = ({ previous, onJoin, loading, error }: JoinQueueProps) => {
  const note = previous ? MESSAGES[previous.estado] : undefined
  return (
    <div className="flex flex-col gap-4">
      {note && (
        <Alert tone={note.tone} title={note.title}>
          {note.text}
        </Alert>
      )}
      {error && (
        <Alert tone="error" title="No pudimos ponerte en la fila">
          {error}
        </Alert>
      )}
      <Button size="lg" loading={loading} onClick={onJoin}>
        Entrar a la fila para comprar
      </Button>
    </div>
  )
}
