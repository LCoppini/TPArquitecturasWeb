import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '@/components/ui/Logo'

type AuthSplitLayoutProps = {
  /** Panel de marca de la derecha. */
  aside: ReactNode
  /** Formulario de la izquierda. */
  children: ReactNode
}

/** Pantalla completa en dos columnas iguales: formulario a la izquierda y panel de marca a la derecha, sin barra ni pie. */
export const AuthSplitLayout = ({ aside, children }: AuthSplitLayoutProps) => (
  <main className="grid min-h-screen grid-cols-2 gap-6 p-6">
    <div className="flex flex-col gap-6 px-10 py-4">
      <Link to="/" aria-label="TicketYa, ir al inicio" className="w-fit no-underline">
        <Logo className="text-[30px]" />
      </Link>
      <div className="flex flex-1 items-center justify-center">{children}</div>
    </div>
    {aside}
  </main>
)
