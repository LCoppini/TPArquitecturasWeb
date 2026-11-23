import type { EventDetail } from '../types/detail'

/** Artistas, equipos o elenco que participan del evento. */
export const EventLineup = ({ lineup }: { lineup: EventDetail['lineup'] }) => {
  if (lineup.length === 0) return null
  return (
    <section aria-labelledby="lineup-title" className="flex flex-col gap-3">
      <h2 id="lineup-title" className="text-h3 text-fg">
        Participan
      </h2>
      <ul className="flex flex-wrap gap-2">
        {lineup.map(({ id, name }) => (
          <li key={id} className="rounded-pill border border-line bg-surface-alt px-4 py-1.5 text-[14px] text-fg">
            {name}
          </li>
        ))}
      </ul>
    </section>
  )
}
