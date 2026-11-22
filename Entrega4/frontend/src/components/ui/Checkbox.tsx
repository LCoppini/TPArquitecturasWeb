import type { ComponentProps, ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { Icon } from './Icon'

type CheckboxProps = Omit<ComponentProps<'input'>, 'type' | 'children'> & {
  label: ReactNode
  invalid?: boolean
}

/** Casilla con etiqueta. El input nativo queda invisible encima de la caja para conservar teclado y foco. */
export const Checkbox = ({ label, invalid = false, className, ref, ...props }: CheckboxProps) => (
  <label className={cn('relative inline-flex cursor-pointer items-start gap-3 text-[15px] leading-[22px] text-fg', className)}>
    <input ref={ref} type="checkbox" aria-invalid={invalid || undefined} className="peer absolute m-0 size-[22px] cursor-pointer opacity-0" {...props} />
    <span
      aria-hidden="true"
      className={cn(
        'grid size-[22px] flex-none place-items-center rounded-[6px] border-[1.5px] border-line-control bg-field text-transparent motion-fast',
        'transition-[background-color,border-color] peer-checked:border-brand peer-checked:bg-brand peer-checked:text-on-brand',
        'peer-focus-visible:outline-3 peer-focus-visible:outline-offset-3 peer-focus-visible:outline-ring',
        invalid && 'border-error-border bg-error-bg',
      )}
    >
      <Icon name="check" className="size-4 stroke-[2.5]" />
    </span>
    <span>{label}</span>
  </label>
)
