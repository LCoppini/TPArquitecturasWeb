import { cva, type VariantProps } from 'class-variance-authority'
import type { ReactNode } from 'react'
import { Icon } from './Icon'
import type { IconName } from './iconPaths'

const badgeVariants = cva(
  'inline-flex h-[26px] items-center gap-1.5 rounded-pill border px-2.5 text-[12px] leading-4 font-semibold whitespace-nowrap',
  {
    variants: {
      tone: {
        success: 'border-success-border bg-success-bg text-success-fg',
        warning: 'border-warning-border bg-warning-bg text-warning-fg',
        error: 'border-error-border bg-error-bg text-error-fg',
        info: 'border-info-border bg-info-bg text-info-fg',
        neutral: 'border-line bg-surface-alt text-fg-secondary',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
)

type BadgeProps = VariantProps<typeof badgeVariants> & {
  /** Punto de color a la izquierda (estados "vivos", como Disponible). */
  dot?: boolean
  icon?: IconName
  children: ReactNode
}

export const Badge = ({ tone, dot = false, icon, children }: BadgeProps) => (
  <span className={badgeVariants({ tone })}>
    {dot && <span aria-hidden="true" className="size-[7px] rounded-full bg-current" />}
    {icon && <Icon name={icon} className="size-3.5 stroke-[2.25]" />}
    {children}
  </span>
)
