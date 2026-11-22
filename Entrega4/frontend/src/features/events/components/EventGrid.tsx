import type { ReactNode } from 'react'

/** Grilla de 4 columnas del catálogo. Recibe tarjetas o skeletons como hijos. */
export const EventGrid = ({ children, busy = false }: { children: ReactNode; busy?: boolean }) => (
  <div aria-busy={busy} className="grid grid-cols-4 items-start gap-6">
    {children}
  </div>
)
