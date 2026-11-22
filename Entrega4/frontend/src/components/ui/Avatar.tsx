import { cn } from '@/lib/cn'

/** Círculo con las iniciales de la persona. Decorativo: el nombre accesible lo da el botón que lo contiene. */
export const Avatar = ({ initials, className }: { initials: string; className?: string }) => (
  <span
    aria-hidden="true"
    className={cn('grid size-10 flex-none place-items-center rounded-full bg-brand text-[14px] leading-none font-bold text-on-brand', className)}
  >
    {initials}
  </span>
)
