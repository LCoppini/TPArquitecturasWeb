import { Link } from 'react-router-dom'
import { cn } from '@/lib/cn'

export type NavbarLinkItem = {
  label: string
  to: string
  active: boolean
}

/** Enlaces principales. El activo se marca con `aria-current="page"` y fondo invertido. */
export const NavbarLinks = ({ items }: { items: NavbarLinkItem[] }) => (
  <>
    {items.map(({ label, to, active }) => (
      <Link
        key={label}
        to={to}
        aria-current={active ? 'page' : undefined}
        className={cn(
          'inline-flex items-center rounded-pill px-4 py-2.5 text-[15px] leading-5 font-medium text-fg-secondary no-underline hover:bg-surface-alt hover:text-fg',
          active && 'bg-inverse text-fg-inverse hover:bg-inverse hover:text-fg-inverse',
        )}
      >
        {label}
      </Link>
    ))}
  </>
)
