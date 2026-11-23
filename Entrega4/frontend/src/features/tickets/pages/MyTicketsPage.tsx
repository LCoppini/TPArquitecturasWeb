import { Link } from 'react-router-dom'
import { EmptyState } from '@/components/feedback/EmptyState'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { TicketGroupSection } from '../components/TicketGroupSection'
import { useMyTickets } from '../hooks/useMyTickets'
import { groupByPurchase } from '../utils/ticketStatus'

export const MyTicketsPage = () => {
  const { data, isPending, error, refetch } = useMyTickets()
  return (
    <Container className="flex max-w-3xl flex-col gap-8 pt-4">
      <h1 className="text-h2 text-fg">Mis entradas</h1>
      {isPending && (
        <div aria-busy="true" className="flex flex-col gap-3">
          <Skeleton className="h-32 rounded-lg" />
          <Skeleton className="h-32 rounded-lg" />
        </div>
      )}
      {error && <ErrorState title="No pudimos cargar tus entradas" description={error.message} onRetry={() => void refetch()} />}
      {data && data.length === 0 && (
        <EmptyState
          icon="ticket"
          title="Todavía no tenés entradas"
          description="Cuando compres, tus entradas con código QR aparecen acá."
          action={
            <Button asChild>
              <Link to="/">Explorar eventos</Link>
            </Button>
          }
        />
      )}
      {data && groupByPurchase(data).map((group) => <TicketGroupSection key={group.key} group={group} />)}
    </Container>
  )
}
