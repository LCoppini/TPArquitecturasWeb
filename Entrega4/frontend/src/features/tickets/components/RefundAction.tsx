import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import type { useRefundPurchase } from '../hooks/useRefundPurchase'

type RefundActionProps = {
  purchaseId: number
  /** Recibe el error del backend (por ejemplo, "faltan menos de 72 horas") para mostrarlo fuera del botón. */
  refund: ReturnType<typeof useRefundPurchase>
}

/** Botón "Devolver compra" con confirmación en el mismo lugar, para no pedir la devolución por error. */
export const RefundAction = ({ purchaseId, refund }: RefundActionProps) => {
  const [confirming, setConfirming] = useState(false)
  if (!confirming) {
    return (
      <Button variant="outline" size="sm" onClick={() => setConfirming(true)}>
        Devolver compra
      </Button>
    )
  }
  return (
    <div className="flex gap-2">
      <Button variant="text" size="sm" onClick={() => setConfirming(false)}>
        Cancelar
      </Button>
      <Button size="sm" loading={refund.isPending} onClick={() => refund.mutate(purchaseId, { onSettled: () => setConfirming(false) })}>
        Confirmar devolución
      </Button>
    </div>
  )
}
