import type { ReactNode } from 'react'
import { SessionExpiredNotice } from './SessionExpiredNotice'

type AuthFormPanelProps = {
  title: string
  /** Texto de apoyo bajo el título, normalmente con el enlace a la otra pantalla. */
  subtitle: ReactNode
  children: ReactNode
}

/** Columna del formulario de ingreso y registro: aviso de sesión vencida, título, bajada y formulario. */
export const AuthFormPanel = ({ title, subtitle, children }: AuthFormPanelProps) => (
  <section aria-labelledby="auth-title" className="flex w-full justify-center py-6">
    <div className="flex w-full max-w-120 flex-col gap-8">
      <SessionExpiredNotice bare />
      <div className="flex flex-col gap-2">
        <h1 id="auth-title" className="text-h1 text-fg">
          {title}
        </h1>
        <p className="text-fg-secondary">{subtitle}</p>
      </div>
      {children}
    </div>
  </section>
)
