import { Slot } from '@radix-ui/react-slot'
import type { VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/cn'
import { buttonVariants } from './buttonVariants'
import { Spinner } from './Spinner'

export type ButtonProps = ComponentProps<'button'> &
  VariantProps<typeof buttonVariants> & {
    /** Renderiza el hijo (por ejemplo un `Link`) con los estilos del botón. */
    asChild?: boolean
    loading?: boolean
  }

export const Button = ({ variant, size, asChild = false, loading = false, className, children, disabled, ...props }: ButtonProps) => {
  const classes = cn(buttonVariants({ variant, size }), loading && 'pointer-events-none cursor-progress opacity-90', className)
  if (asChild) return <Slot className={classes} {...props}>{children}</Slot>
  return (
    <button
      type="button"
      className={classes}
      disabled={disabled}
      aria-busy={loading || undefined}
      aria-disabled={loading || undefined}
      {...props}
      onClick={(event) => (loading ? event.preventDefault() : props.onClick?.(event))}
    >
      {loading && <Spinner />}
      {children}
    </button>
  )
}
