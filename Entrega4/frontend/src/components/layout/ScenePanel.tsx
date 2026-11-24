import type { ComponentProps } from 'react'
import { cn } from '@/lib/cn'
import { SceneArt } from './SceneArt'

type ScenePanelProps = ComponentProps<'div'> & {
  /** Posición de la ilustración dentro del panel (clases de Tailwind). */
  artClassName?: string
}

/** Panel oscuro con fondo de escena. Todo su contenido usa el tema oscuro del design system. */
export const ScenePanel = ({ artClassName, className, children, ...props }: ScenePanelProps) => (
  <div data-theme="dark" className={cn('relative overflow-hidden rounded-2xl bg-scene text-fg', className)} {...props}>
    <SceneArt className={artClassName} />
    {children}
  </div>
)
