import { cn } from '@/lib/cn'

/** Bloque de carga con brillo animado. Es decorativo: la región que lo contiene debe indicar `aria-busy`. */
export const Skeleton = ({ className }: { className?: string }) => (
  <div
    aria-hidden="true"
    className={cn(
      'animate-[ty-shimmer_1.4s_linear_infinite] rounded-sm bg-[length:200%_100%]',
      'bg-[linear-gradient(100deg,var(--ty-bg-surface-alt)_30%,var(--ty-border-subtle)_50%,var(--ty-bg-surface-alt)_70%)]',
      className,
    )}
  />
)
