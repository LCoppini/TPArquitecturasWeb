import { Link } from 'react-router-dom'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'

type PaymentOutcomeProps = {
  /** Estado de la Compra tras el pago: PAGADA, RECHAZADA, VENCIDA o RESERVADA (pago en demora). */
  status: string
  onRetry: () => void
  onRefresh: () => void
}

/** Resultado del pago con el siguiente paso claro para cada caso. */
export const PaymentOutcome = ({ status, onRetry, onRefresh }: PaymentOutcomeProps) => {
  if (status === 'PAGADA') {
    return (
      <div className="flex flex-col gap-4">
        <Alert tone="success" title="¡Compra confirmada!">
          Tus entradas con código QR ya están disponibles. También te enviamos un correo.
        </Alert>
        <Button asChild size="lg">
          <Link to="/mis-entradas">Ver mis entradas</Link>
        </Button>
      </div>
    )
  }
  if (status === 'RESERVADA') {
    return (
      <div className="flex flex-col gap-4">
        <Alert tone="warning" title="Tu pago está en proceso">
          Estamos esperando la confirmación del medio de pago. Consultá de nuevo en unos instantes.
        </Alert>
        <Button variant="outline" onClick={onRefresh}>
          Actualizar estado
        </Button>
      </div>
    )
  }
  const expired = status === 'VENCIDA'
  return (
    <div className="flex flex-col gap-4">
      <Alert tone="error" title={expired ? 'La reserva venció' : 'Pago rechazado'}>
        {expired ? 'No llegaste a pagar a tiempo. Podés volver a ponerte en la fila.' : 'No se cobró nada. Podés intentar de nuevo.'}
      </Alert>
      <Button variant="outline" onClick={onRetry}>
        Volver a intentar
      </Button>
    </div>
  )
}
