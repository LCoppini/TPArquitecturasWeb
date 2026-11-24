import type { ReactNode } from 'react'
import { Button } from '@/components/ui/Button'
import { Icon } from '@/components/ui/Icon'

type ErrorStateProps = {
  title: string
  description: string
  /** Si se indica, muestra el botón "Reintentar". */
  onRetry?: () => void
  /** Acción adicional (por ejemplo un enlace al inicio). */
  action?: ReactNode
}

/** Pantalla o bloque de error con una salida clara: reintentar o volver. */
export const ErrorState = ({ title, description, onRetry, action }: ErrorStateProps) => (
  <div role="alert" className="mx-auto flex max-w-md flex-col items-center gap-4 py-16 text-center">
    <span className="grid size-14 place-items-center rounded-full bg-error-bg text-error-fg">
      <Icon name="alertCircle" className="size-7" />
    </span>
    <h2 className="text-h3 text-fg">{title}</h2>
    <p className="text-body-m text-fg-secondary">{description}</p>
    <div className="flex gap-3">
      {onRetry && <Button onClick={onRetry}>Reintentar</Button>}
      {action}
    </div>
  </div>
)
