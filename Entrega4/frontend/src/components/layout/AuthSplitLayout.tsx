import type { ReactNode } from 'react'
import { Container } from './Container'

type AuthSplitLayoutProps = {
  /** Panel de marca de la izquierda. */
  aside: ReactNode
  /** Formulario de la derecha. */
  children: ReactNode
}

/** Dos columnas iguales: panel de marca y formulario (ingreso y registro). */
export const AuthSplitLayout = ({ aside, children }: AuthSplitLayoutProps) => (
  <Container className="grid grid-cols-2 items-stretch gap-6 pt-4">
    {aside}
    {children}
  </Container>
)
