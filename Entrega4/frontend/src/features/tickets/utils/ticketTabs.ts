import type { TicketGroup } from './ticketStatus'

export type TicketTab = 'all' | 'upcoming' | 'past' | 'returned'
type Section = Exclude<TicketTab, 'all'>

export const TICKET_TABS: { id: TicketTab; label: string }[] = [
  { id: 'all', label: 'Todas' },
  { id: 'upcoming', label: 'Próximas' },
  { id: 'past', label: 'Pasadas' },
  { id: 'returned', label: 'Devueltas' },
]

const RETURNED = new Set(['DEVUELTA', 'EN_REINTEGRO'])
const FINISHED = new Set(['USADA', 'VENCIDA', 'CANCELADA'])

/** Fecha de inicio por evento, cuando ya se pudo consultar. */
export type StartDates = ReadonlyMap<number, string>

const sectionOfTicket = (estado: string, startsAt: string | undefined, now: number): Section => {
  if (RETURNED.has(estado)) return 'returned'
  if (FINISHED.has(estado)) return 'past'
  return startsAt !== undefined && new Date(startsAt).getTime() < now ? 'past' : 'upcoming'
}

/** Una compra va a "Próximas" si le queda alguna entrada por usar; si no, a "Pasadas" o, si solo hay devoluciones, a "Devueltas". */
export const sectionOfGroup = ({ tickets }: TicketGroup, startDates: StartDates, now: number): Section => {
  const sections = tickets.map(({ estado, idEvento }) => sectionOfTicket(estado, startDates.get(idEvento), now))
  if (sections.includes('upcoming')) return 'upcoming'
  return sections.includes('past') ? 'past' : 'returned'
}

export const filterGroups = (groups: TicketGroup[], tab: TicketTab, startDates: StartDates, now: number): TicketGroup[] =>
  tab === 'all' ? groups : groups.filter((group) => sectionOfGroup(group, startDates, now) === tab)

export const countGroups = (groups: TicketGroup[], startDates: StartDates, now: number): Record<TicketTab, number> => {
  const counts: Record<TicketTab, number> = { all: groups.length, upcoming: 0, past: 0, returned: 0 }
  for (const group of groups) counts[sectionOfGroup(group, startDates, now)] += 1
  return counts
}
