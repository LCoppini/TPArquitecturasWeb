import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/Button'

/** Error general (500): no depende del router porque puede mostrarse con la app rota. */
export const ServerErrorPage = ({ onRetry }: { onRetry?: () => void }) => (
  <Container>
    <ErrorState
      title="Algo salió de nuestro lado"
      description="Tuvimos un problema inesperado. Probá de nuevo; si sigue pasando, volvé al inicio."
      onRetry={onRetry}
      action={
        <Button asChild variant="outline">
          <a href="/">Volver al inicio</a>
        </Button>
      }
    />
  </Container>
)
