import { POSTER_ARTS } from './posterArts'

/** Arte del evento: se elige una de las ilustraciones según el id, para que cada evento conserve la suya. */
export const EventPoster = ({ eventId }: { eventId: number }) => (
  <svg viewBox="0 0 300 190" preserveAspectRatio="xMidYMid slice" aria-hidden="true" className="absolute inset-0 size-full">
    {POSTER_ARTS[Math.abs(eventId) % POSTER_ARTS.length]}
  </svg>
)
