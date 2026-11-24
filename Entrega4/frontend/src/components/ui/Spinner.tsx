import { cn } from '@/lib/cn'

/** Indicador de carga para botones y paneles. El significado lo da el texto que lo acompaña. */
export const Spinner = ({ className }: { className?: string }) => (
  <span
    aria-hidden="true"
    className={cn('inline-block size-4 animate-spin rounded-full border-2 border-current border-t-transparent', className)}
  />
)
