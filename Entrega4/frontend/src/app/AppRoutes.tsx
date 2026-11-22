import { Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { HomePage } from '@/features/events/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { AppLayout } from './AppLayout'

export const AppRoutes = () => (
  <Routes>
    <Route element={<AppLayout />}>
      <Route index element={<HomePage />} />
      <Route path="ingresar" element={<LoginPage />} />
      <Route path="registro" element={<RegisterPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Route>
  </Routes>
)
