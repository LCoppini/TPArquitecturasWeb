import type { ReactNode } from 'react'
import { Icon } from '@/components/ui/Icon'
import type { IconName } from '@/components/ui/iconPaths'

type EmptyStateProps = {
  icon?: IconName
  title: string
  description: string
  action?: ReactNode
}

/** Bloque para listas sin resultados, con una acción para salir de ahí. */
export const EmptyState = ({ icon = 'search', title, description, action }: EmptyStateProps) => (
  <div className="mx-auto flex max-w-md flex-col items-center gap-4 py-16 text-center">
    <span className="grid size-14 place-items-center rounded-full bg-surface-alt text-fg-secondary">
      <Icon name={icon} className="size-7" />
    </span>
    <h2 className="text-h3 text-fg">{title}</h2>
    <p className="text-body-m text-fg-secondary">{description}</p>
    {action}
  </div>
)
