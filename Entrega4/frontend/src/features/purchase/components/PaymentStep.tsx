import { useState } from 'react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Countdown } from './Countdown'
import { PurchaseSummary } from './PurchaseSummary'
import type { CompraApi, PurchaseSelection } from '../types'

const METHODS = ['Tarjeta de crédito', 'Tarjeta de débito', 'Mercado Pago']

type PaymentStepProps = {
  purchase: CompraApi
  selection: PurchaseSelection
  onPay: (method: string) => void
  loading: boolean
  error: string | null
}

/** Entradas reservadas: elijo medio de pago y pago antes de que venza la reserva. */
export const PaymentStep = ({ purchase, selection, onPay, loading, error }: PaymentStepProps) => {
  const [method, setMethod] = useState(METHODS[0])
  return (
    <div className="flex flex-col gap-4">
      <Alert tone="info" title="Entradas reservadas">
        <Countdown until={purchase.venceEn} label="Pagá antes de que venza la reserva:" />
      </Alert>
      <PurchaseSummary selection={selection} total={purchase.total} />
      <label className="flex flex-col gap-1.5 text-[14px] font-medium text-fg">
        Medio de pago
        <select
          value={method}
          onChange={(event) => setMethod(event.target.value)}
          className="h-12 rounded-md border border-line bg-surface px-3 text-[15px] text-fg"
        >
          {METHODS.map((option) => (
            <option key={option}>{option}</option>
          ))}
        </select>
      </label>
      {error && (
        <Alert tone="error" title="No pudimos procesar el pago">
          {error}
        </Alert>
      )}
      <Button size="lg" loading={loading} onClick={() => onPay(method)}>
        Pagar
      </Button>
    </div>
  )
}
