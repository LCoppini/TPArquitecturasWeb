import type { ComponentProps } from 'react'
import { cn } from '@/lib/cn'

/** Centra el contenido con el ancho máximo del diseño (1280 px sobre 1440). */
export const Container = ({ className, ...props }: ComponentProps<'div'>) => (
  <div className={cn('mx-auto w-full max-w-content px-6 min-[1328px]:px-0', className)} {...props} />
)
