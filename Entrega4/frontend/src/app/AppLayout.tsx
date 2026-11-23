import { Outlet } from 'react-router-dom'
import { Container } from '@/components/layout/Container'
import { Footer } from '@/components/layout/Footer'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { AppNavbar } from './AppNavbar'
import { footerColumns } from './navigation'

export const AppLayout = () => {
  const { session } = useAuth()
  return (
    <div className="flex min-h-screen flex-col gap-6 pt-4 pb-6">
      <Container>
        <AppNavbar />
      </Container>
      <main className="flex-1">
        <Outlet />
      </main>
      <Container>
        <Footer columns={footerColumns(session !== null)} />
      </Container>
    </div>
  )
}
