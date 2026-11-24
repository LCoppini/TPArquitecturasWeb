import { Chip } from '@/components/ui/Chip'
import { TICKET_TABS, type TicketTab } from '../utils/ticketTabs'

type TicketTabsProps = {
  active: TicketTab
  counts: Record<TicketTab, number>
  onChange: (tab: TicketTab) => void
}

/** Filtros de la lista de compras: todas, próximas, pasadas o devueltas. */
export const TicketTabs = ({ active, counts, onChange }: TicketTabsProps) => (
  <div role="group" aria-label="Filtrar entradas" className="flex flex-wrap gap-2">
    {TICKET_TABS.map(({ id, label }) => (
      <Chip key={id} className="h-9 px-3.5 text-[13px]" pressed={active === id} count={counts[id]} onClick={() => onChange(id)}>
        {label}
      </Chip>
    ))}
  </div>
)
