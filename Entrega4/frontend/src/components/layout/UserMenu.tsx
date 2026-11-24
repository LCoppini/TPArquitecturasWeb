import { Link } from 'react-router-dom'
import { useCallback, useRef, useState } from 'react'
import { Avatar } from '@/components/ui/Avatar'
import { Icon } from '@/components/ui/Icon'
import type { IconName } from '@/components/ui/iconPaths'
import { useDismiss } from '@/lib/hooks/useDismiss'

type UserMenuProps = {
  email: string
  onSignOut: () => void
}

const MenuLink = ({ to, icon, label, onNavigate }: { to: string; icon: IconName; label: string; onNavigate: () => void }) => (
  <Link
    to={to}
    role="menuitem"
    onClick={onNavigate}
    className="flex items-center gap-2 rounded-sm px-3 py-2.5 text-[15px] leading-5 text-fg no-underline hover:bg-surface-alt"
  >
    <Icon name={icon} />
    {label}
  </Link>
)

const initialsOf = (email: string) => email.slice(0, 2).toUpperCase()

/** Menú de cuenta de la barra. Hoy muestra el correo de la sesión y la opción de cerrar sesión. */
export const UserMenu = ({ email, onSignOut }: UserMenuProps) => {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const close = useCallback(() => setOpen(false), [])
  useDismiss(rootRef, close, open)

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Menú de tu cuenta"
        onClick={() => setOpen((current) => !current)}
        className="inline-flex min-h-11 cursor-pointer items-center gap-2 rounded-pill p-1 pr-3 text-fg hover:bg-surface-alt"
      >
        <Avatar initials={initialsOf(email)} />
        <Icon name="chevronDown" />
      </button>
      {open && (
        <div role="menu" aria-label="Cuenta" className="absolute top-full right-0 z-(--ty-z-dropdown) mt-2 flex w-64 flex-col gap-0.5 rounded-md border border-line-subtle bg-surface p-1.5 shadow-md">
          <div className="flex items-center gap-3 px-3 py-2.5">
            <Avatar initials={initialsOf(email)} className="size-12" />
            <span className="min-w-0 truncate text-[14px] leading-5 text-fg-secondary">{email}</span>
          </div>
          <div role="separator" className="my-1 h-px bg-line-subtle" />
          <MenuLink to="/mis-entradas" icon="ticket" label="Mis entradas" onNavigate={close} />
          <MenuLink to="/notificaciones" icon="mail" label="Notificaciones" onNavigate={close} />
          <MenuLink to="/perfil" icon="user" label="Mi perfil" onNavigate={close} />
          <div role="separator" className="my-1 h-px bg-line-subtle" />
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              close()
              onSignOut()
            }}
            className="flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2.5 text-left text-[15px] leading-5 text-fg hover:bg-surface-alt"
          >
            <Icon name="logout" />
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  )
}
