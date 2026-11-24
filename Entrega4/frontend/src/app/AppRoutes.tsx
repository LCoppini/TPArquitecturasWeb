import { Route, Routes } from 'react-router-dom'
import { GuestOnly } from '@/features/auth/components/GuestOnly'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { RequireAuth } from '@/features/auth/components/RequireAuth'
import { EventDetailPage } from '@/features/events/pages/EventDetailPage'
import { MyTicketsPage } from '@/features/tickets/pages/MyTicketsPage'
import { ProfilePage } from '@/features/profile/pages/ProfilePage'
import { HomePage } from '@/features/events/pages/HomePage'
import { NotificationsPage } from '@/features/notifications/pages/NotificationsPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { AppLayout } from './AppLayout'

export const AppRoutes = () => (
  <Routes>
    <Route element={<AppLayout />}>
      <Route index element={<HomePage />} />
      <Route path="eventos/:id" element={<EventDetailPage />} />
      <Route element={<RequireAuth />}>
        <Route path="mis-entradas" element={<MyTicketsPage />} />
        <Route path="perfil" element={<ProfilePage />} />
        <Route path="notificaciones" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Route>
    {/* Ingreso y registro ocupan toda la pantalla: sin barra ni pie. */}
    <Route element={<GuestOnly />}>
      <Route path="ingresar" element={<LoginPage />} />
      <Route path="registro" element={<RegisterPage />} />
    </Route>
  </Routes>
)
