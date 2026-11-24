import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/** Rutas de ingreso y registro: quien ya tiene sesión vuelve al inicio. */
export const GuestOnly = () => {
  const { session } = useAuth()
  return session ? <Navigate to="/" replace /> : <Outlet />
}
