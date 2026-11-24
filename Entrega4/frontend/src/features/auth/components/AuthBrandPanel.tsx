import { Icon } from '@/components/ui/Icon'
import type { IconName } from '@/components/ui/iconPaths'
import { ScenePanel } from '@/components/layout/ScenePanel'

const BENEFITS: { icon: IconName; text: string }[] = [
  { icon: 'ticket', text: 'Recibí tus entradas al instante, siempre a mano.' },
  { icon: 'shield', text: 'Pagá seguro, con el precio final desde el principio.' },
  { icon: 'clock', text: 'Devolvé tus entradas hasta 48 horas antes.' },
]

/** Panel oscuro de marca que acompaña a los formularios de ingreso y registro. */
export const AuthBrandPanel = () => (
  <ScenePanel aria-label="Por qué TicketYa" className="flex flex-col justify-center gap-16 p-16" artClassName="-top-32 -right-40">
    <div className="relative flex max-w-120 flex-col gap-6">
      <span className="text-overline text-brand-text">Entradas en pocos pasos</span>
      <p className="text-display text-fg">Tu noche, tu entrada</p>
      <p className="text-body-l text-fg-secondary">Recitales, teatro y deportes en un solo lugar.</p>
    </div>
    <ul className="relative flex flex-col gap-3.5">
      {BENEFITS.map(({ icon, text }) => (
        <li key={text} className="flex items-center gap-3 text-fg-secondary">
          <Icon name={icon} className="text-brand-text" />
          {text}
        </li>
      ))}
    </ul>
  </ScenePanel>
)
