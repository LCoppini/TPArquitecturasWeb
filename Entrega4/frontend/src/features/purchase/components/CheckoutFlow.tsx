import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { purchaseApi } from '../api/purchaseApi'
import { usePurchaseActions } from '../hooks/usePurchaseActions'
import type { CompraApi, IngresoApi, PurchaseSelection } from '../types'
import { describePurchaseError } from '../utils/purchaseErrors'
import { PaymentOutcome } from './PaymentOutcome'
import { PaymentStep } from './PaymentStep'
import { ReserveStep } from './ReserveStep'

type CheckoutFlowProps = {
  eventId: number
  entry: IngresoApi
  selection: PurchaseSelection
}

/** Pasos con turno habilitado: reservar -> pagar -> resultado. */
export const CheckoutFlow = ({ eventId, entry, selection }: CheckoutFlowProps) => {
  const queryClient = useQueryClient()
  const { reserve, pay } = usePurchaseActions(eventId)
  const [purchase, setPurchase] = useState<CompraApi | null>(null)
  const [outcome, setOutcome] = useState<string | null>(null)

  const restart = () => {
    setPurchase(null)
    setOutcome(null)
    void queryClient.invalidateQueries({ queryKey: ['queue-entry', eventId] })
  }
  const refresh = () => {
    if (purchase?.idCompra == null) return
    void purchaseApi.get(purchase.idCompra).then((current) => setOutcome(current.estado))
  }

  if (outcome) return <PaymentOutcome status={outcome} onRetry={restart} onRefresh={refresh} />
  if (purchase?.idCompra != null) {
    return (
      <PaymentStep
        purchase={purchase}
        selection={selection}
        loading={pay.isPending}
        error={pay.error ? describePurchaseError(pay.error) : null}
        onPay={(method) =>
          pay.mutate({ purchaseId: purchase.idCompra as number, method }, { onSuccess: (result) => setOutcome(result.estado) })
        }
      />
    )
  }
  return (
    <ReserveStep
      entry={entry}
      selection={selection}
      loading={reserve.isPending}
      error={reserve.error ? describePurchaseError(reserve.error) : null}
      onReserve={() => reserve.mutate(selection, { onSuccess: setPurchase })}
    />
  )
}
