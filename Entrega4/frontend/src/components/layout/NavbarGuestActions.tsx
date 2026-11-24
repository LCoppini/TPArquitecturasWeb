import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'

/** Acciones de la barra cuando no hay sesión. */
export const NavbarGuestActions = () => (
  <>
    <Button asChild variant="text">
      <Link to="/ingresar">Ingresar</Link>
    </Button>
    <Button asChild>
      <Link to="/registro">Crear cuenta</Link>
    </Button>
  </>
)
