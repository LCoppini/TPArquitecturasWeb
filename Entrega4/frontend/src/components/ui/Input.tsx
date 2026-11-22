import type { ComponentProps, ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { Icon } from './Icon'
import type { IconName } from './iconPaths'

export type InputProps = ComponentProps<'input'> & {
  icon?: IconName
  trailing?: ReactNode
  invalid?: boolean
}

/** Campo de texto con ícono opcional a la izquierda y un elemento opcional a la derecha. */
export const Input = ({ icon, trailing, invalid = false, className, ref, ...props }: InputProps) => (
  <div
    className={cn(
      'flex h-12 w-full items-center gap-2.5 rounded-md border-[1.5px] border-line-control bg-field px-4 text-[16px] leading-6 text-fg motion-fast',
      'transition-[border-color,box-shadow] hover:border-fg-secondary',
      'focus-within:border-ring focus-within:bg-surface focus-within:shadow-[0_0_0_3px_var(--ty-accent-soft),0_0_0_1px_var(--ty-focus-ring)]',
      invalid && 'border-error-border bg-error-bg',
      className,
    )}
  >
    {icon && <Icon name={icon} className={invalid ? 'text-error-fg' : 'text-fg-secondary'} />}
    <input
      ref={ref}
      aria-invalid={invalid || undefined}
      className="h-full min-w-0 flex-1 bg-transparent outline-none placeholder:text-fg-muted"
      {...props}
    />
    {trailing}
  </div>
)
