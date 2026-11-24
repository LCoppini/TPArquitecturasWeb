import type { ComponentProps, ReactNode } from 'react'
import { cn } from '@/lib/cn'

type ChipProps = Omit<ComponentProps<'button'>, 'children'> & {
  pressed?: boolean
  count?: number
  children: ReactNode
}

/** Botón de filtro tipo píldora. `pressed` lo marca como seleccionado (`aria-pressed`). */
export const Chip = ({ pressed = false, count, className, children, ...props }: ChipProps) => (
  <button
    type="button"
    aria-pressed={pressed}
    className={cn(
      'inline-flex h-10 cursor-pointer items-center gap-2 rounded-pill border-[1.5px] border-line bg-surface px-4 text-[14px] leading-5 font-medium text-fg motion-fast',
      'transition-[background-color,border-color] hover:border-line-strong hover:bg-surface-alt',
      'aria-pressed:border-brand aria-pressed:bg-brand aria-pressed:font-semibold aria-pressed:text-on-brand aria-pressed:hover:bg-brand-hover',
      'disabled:cursor-not-allowed disabled:border-dashed disabled:border-line-subtle disabled:bg-surface-alt disabled:text-fg-disabled',
      className,
    )}
    {...props}
  >
    {children}
    {count !== undefined && (
      <span className="rounded-pill bg-surface-alt px-2 py-0.5 text-[12px] leading-4 font-semibold text-fg-secondary [[aria-pressed=true]_&]:bg-ink/15 [[aria-pressed=true]_&]:text-on-brand">
        {count}
      </span>
    )}
  </button>
)
