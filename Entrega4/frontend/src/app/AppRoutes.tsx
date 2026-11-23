import { Route, Routes } from 'react-router-dom'
import { GuestOnly } from '@/features/auth/components/GuestOnly'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { HomePage } from '@/features/events/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { AppLayout } from './AppLayout'

export const AppRoutes = () => (
  <Routes>
    <Route element={<AppLayout />}>
      <Route index element={<HomePage />} />
      <Route element={<GuestOnly />}>
        <Route path="ingresar" element={<LoginPage />} />
        <Route path="registro" element={<RegisterPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Route>
  </Routes>
)
