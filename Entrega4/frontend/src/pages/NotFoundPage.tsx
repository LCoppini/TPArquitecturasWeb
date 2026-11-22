import { Link } from 'react-router-dom'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/Button'

export const NotFoundPage = () => (
  <Container>
    <ErrorState
      title="No encontramos esta página"
      description="La dirección no existe o cambió de lugar."
      action={
        <Button asChild>
          <Link to="/">Volver al inicio</Link>
        </Button>
      }
    />
  </Container>
)
