/** Los eventos se muestran siempre en horario de Argentina, sin importar la zona del navegador. */
const EVENT_TIME_ZONE = 'America/Argentina/Buenos_Aires'

const PARTS_FORMAT = new Intl.DateTimeFormat('es-AR', {
  timeZone: EVENT_TIME_ZONE,
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)
const withoutDot = (text: string) => text.replace(/\.$/, '')

const partsOf = (iso: string): Record<string, string> =>
  Object.fromEntries(PARTS_FORMAT.formatToParts(new Date(iso)).map(({ type, value }) => [type, value]))

/** "2026-11-28T00:00:00Z" -> "Sáb 28 nov · 21:00" */
export const formatEventDate = (iso: string): string => {
  const { weekday, day, month, hour, minute } = partsOf(iso)
  return `${capitalize(withoutDot(weekday))} ${day} ${withoutDot(month)} · ${hour}:${minute}`
}

/** Día y mes abreviado para la insignia de fecha de la tarjeta: { day: "28", month: "NOV" }. */
export const formatEventDay = (iso: string): { day: string; month: string } => {
  const { day, month } = partsOf(iso)
  return { day, month: withoutDot(month).toUpperCase() }
}
