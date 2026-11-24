import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { Icon } from './Icon'

type FieldProps = {
  id: string
  label: string
  help?: string
  error?: string
  className?: string
  children: ReactNode
}

/** Etiqueta + control + ayuda o error. El id del control debe coincidir con `id`. */
export const Field = ({ id, label, help, error, className, children }: FieldProps) => (
  <div className={cn('flex min-w-0 flex-col gap-1.5', className)}>
    <label htmlFor={id} className="text-[14px] leading-5 font-semibold text-fg">
      {label}
    </label>
    {children}
    {error ? (
      <p id={`${id}-error`} className="flex items-start gap-1.5 text-[13px] leading-[18px] font-medium text-error-fg">
        <Icon name="alertCircle" className="mt-px size-4" />
        {error}
      </p>
    ) : (
      help && (
        <p id={`${id}-help`} className="text-[13px] leading-[18px] text-fg-muted">
          {help}
        </p>
      )
    )}
  </div>
)
