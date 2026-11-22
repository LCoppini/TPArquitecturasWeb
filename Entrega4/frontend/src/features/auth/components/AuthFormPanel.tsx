import type { ReactNode } from 'react'

type AuthFormPanelProps = {
  title: string
  /** Texto de apoyo bajo el título, normalmente con el enlace a la otra pantalla. */
  subtitle: ReactNode
  children: ReactNode
}

/** Columna derecha de las pantallas de ingreso y registro: título, bajada y formulario. */
export const AuthFormPanel = ({ title, subtitle, children }: AuthFormPanelProps) => (
  <section aria-labelledby="auth-title" className="flex items-center justify-center py-12">
    <div className="flex w-full max-w-110 flex-col gap-6">
      <div className="flex flex-col gap-2">
        <h1 id="auth-title" className="text-h2 text-fg">
          {title}
        </h1>
        <p className="text-fg-secondary">{subtitle}</p>
      </div>
      {children}
    </div>
  </section>
)
