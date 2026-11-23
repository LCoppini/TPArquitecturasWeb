import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/** Rutas que piden sesión: sin ella, lleva al ingreso y recuerda a dónde volver. */
export const RequireAuth = () => {
  const { session } = useAuth()
  const location = useLocation()
  if (session) return <Outlet />
  return <Navigate to="/ingresar" replace state={{ from: `${location.pathname}${location.search}` }} />
}
