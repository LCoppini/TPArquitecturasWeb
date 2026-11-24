import { Link } from 'react-router-dom'
import { Container } from '@/components/layout/Container'
import { Alert } from '@/components/ui/Alert'
import { useAuth } from '../hooks/useAuth'

/** Aviso cuando la sesión venció: pide ingresar de nuevo sin perder el lugar donde estaba la persona. `bare` lo muestra sin el contenedor de página. */
export const SessionExpiredNotice = ({ bare = false }: { bare?: boolean }) => {
  const { sessionExpired, isAuthenticated } = useAuth()
  if (!sessionExpired || isAuthenticated) return null
  const alert = (
    <Alert tone="warning" title="Tu sesión venció">
      Por seguridad, volvé a ingresar para seguir.{' '}
      <Link to="/ingresar" className="font-semibold underline">
        Ingresar de nuevo
      </Link>
    </Alert>
  )
  return bare ? alert : <Container>{alert}</Container>
}
