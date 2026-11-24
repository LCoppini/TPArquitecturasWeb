import { useLocation, useSearchParams } from 'react-router-dom'
import { Navbar } from '@/components/layout/Navbar'
import { NavbarGuestActions } from '@/components/layout/NavbarGuestActions'
import { NavbarLinks, type NavbarLinkItem } from '@/components/layout/NavbarLinks'
import { UserMenu } from '@/components/layout/UserMenu'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { CATEGORY_LINKS } from './navigation'

/** Barra superior conectada a la sesión y a la dirección actual. */
export const AppNavbar = () => {
  const { session, signOut } = useAuth()
  const { pathname } = useLocation()
  const [params] = useSearchParams()
  const activeCategory = pathname === '/' ? params.get('categoria') : null

  const items: NavbarLinkItem[] = [
    { label: 'Eventos', to: '/', active: pathname === '/' && !activeCategory },
    ...CATEGORY_LINKS.map(({ label, to, slug }) => ({ label, to, active: activeCategory === slug })),
  ]

  return (
    <Navbar
      links={<NavbarLinks items={items} />}
      actions={session ? <UserMenu email={session.email} onSignOut={signOut} /> : <NavbarGuestActions />}
    />
  )
}
