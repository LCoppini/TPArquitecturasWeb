import { useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '@/components/feedback/EmptyState'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Container } from '@/components/layout/Container'
import { Button } from '@/components/ui/Button'
import { Skeleton } from '@/components/ui/Skeleton'
import { TicketGroupSection } from '../components/TicketGroupSection'
import { TicketTabs } from '../components/TicketTabs'
import { useEventStartDates } from '../hooks/useEventStartDates'
import { useMyTickets } from '../hooks/useMyTickets'
import { groupByPurchase } from '../utils/ticketStatus'
import { countGroups, filterGroups, type TicketTab } from '../utils/ticketTabs'

export const MyTicketsPage = () => {
  const { data, isPending, error, refetch } = useMyTickets()
  const [tab, setTab] = useState<TicketTab>('all')
  const [now] = useState(() => Date.now())
  const startDates = useEventStartDates([...new Set((data ?? []).map(({ idEvento }) => idEvento))])
  const groups = data ? groupByPurchase(data) : []
  const visible = filterGroups(groups, tab, startDates, now)

  return (
    <Container className="flex flex-col gap-6 pt-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-h2 text-fg">Mis entradas</h1>
        {groups.length > 0 && <TicketTabs active={tab} counts={countGroups(groups, startDates, now)} onChange={setTab} />}
        </div>
      {isPending && (
        <div aria-busy="true" className="grid gap-6 lg:grid-cols-2">
          <Skeleton className="h-56 rounded-xl" />
          <Skeleton className="h-56 rounded-xl" />
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
      {groups.length > 0 && visible.length === 0 && (
        <EmptyState icon="ticket" title="No hay compras en esta sección" description="Probá con otra pestaña para ver el resto de tus entradas." />
      )}
      {visible.length > 0 && (
        <div className="grid items-start gap-6 lg:grid-cols-2">
          {visible.map((group) => (
            <TicketGroupSection key={group.key} group={group} />
          ))}
        </div>
      )}
    </Container>
  )
}
