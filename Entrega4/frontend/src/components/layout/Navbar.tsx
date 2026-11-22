import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '@/components/ui/Logo'

type NavbarProps = {
  links: ReactNode
  actions: ReactNode
}

/** Barra superior en forma de píldora: logo, enlaces de navegación y acciones de la cuenta. */
export const Navbar = ({ links, actions }: NavbarProps) => (
  <nav aria-label="Principal" className="relative z-(--ty-z-sticky) flex h-18 items-center gap-8 rounded-pill border border-line-subtle bg-surface px-8 text-fg">
    <Link to="/" aria-label="TicketYa, ir al inicio">
      <Logo />
    </Link>
    <div className="flex flex-1 gap-1">{links}</div>
    <div className="flex items-center gap-2">{actions}</div>
  </nav>
)
