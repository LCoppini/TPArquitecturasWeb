import { formatEventDay } from '@/lib/format/date'

/** Insignia de vidrio con el día y el mes abreviado, sobre el póster. */
export const EventDateBadge = ({ startsAt }: { startsAt: string }) => {
  const { day, month } = formatEventDay(startsAt)
  return (
    <div aria-hidden="true" className="surface-glass absolute top-3.5 left-3.5 flex flex-col items-center rounded-md px-3 py-1.5 text-center text-[20px] leading-[22px] font-bold text-bone">
      <span className="text-[11px] leading-3.5 font-semibold tracking-[0.12em] text-mint">{month}</span>
      {day}
    </div>
  )
}
